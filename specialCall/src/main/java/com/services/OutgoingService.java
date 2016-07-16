package com.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.receivers.StartStandOutServicesFallBackReceiver;
import com.utils.ContactsUtils;
import com.utils.MCBlockListUtils;
import com.utils.MCHistoryUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.io.File;
import java.io.IOException;

import DataObjects.SpecialMediaType;
import utils.PhoneNumberUtils;
import wei.mark.standout.ui.Window;

import static com.crashlytics.android.Crashlytics.log;

//import android.telephony.PreciseCallState;


/**
 * Created by Mor on 08/01/2016.
 */
public class OutgoingService extends AbstractStandOutService {

    public static boolean isLive = false;
    private static int TIME_TO_SLEEP_AVOIDING_BUGGY_STATE_IDLE = 1000;
    private OutgoingCallReceiver mOutgoingCallReceiver;
    private boolean funtoneFileExists = false;

    public OutgoingService() {
        super(OutgoingService.class.getSimpleName());
    }

    //region Service methods
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        isLive = true;

        registerOutgoingReceiver();
        prepareVideoListener();
        checkIntent(intent);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isLive = false; // Service Is Dead !
        unregisterReceiver(mOutgoingCallReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onShow(int id, Window window) {
        super.onShow(id, window);  // at last so the volume will return to the previous(since when it was showed) , to make the volume always mute after Unhide move it to the Start of the method.

            setVolumeSilentForOutgoingCalls(); // outgoing calls should start in MUTE first
            log(Log.INFO,TAG, "setVolumeSilentForOutgoingCalls");

        return false;
    }
    //endregion

    //region AbstractStandOutService methods
    /**
     * @param state          The call state
     * @param incomingNumber The incoming number in case of an incoming call. Otherwise (outgoing call), null.
     */
    @Override
    protected void syncOnCallStateChange(int state, String incomingNumber) {

        if (!MCBlockListUtils.IsMCBlocked(incomingNumber, getApplicationContext()))

            switch (state) {

                case TelephonyManager.CALL_STATE_IDLE:
                case TelephonyManager.CALL_STATE_OFFHOOK:

                    log(Log.INFO,TAG, "TelephonyManager IDLE=0, OFFHOOK=2. STATE WAS:" + state);
                    if (isRingingSession(SharedPrefUtils.OUTGOING_RINGING_SESSION)) {
                        log(Log.INFO,TAG, "TelephonyManager inside mInRingingSession IDLE=0, OFFHOOK=2. STATE WAS:" + state);

                        try {
                            closeSpecialCallWindowWithoutRingtone();
                         //   resumeMusicStreamBackToPrevious();
                            setRingingSession(SharedPrefUtils.OUTGOING_RINGING_SESSION, false);
                        } finally {
                            releaseResources();
                        }
                    }

            }
    }

    @Override
    protected void playSound(Context context, Uri alert) {

        log(Log.INFO,TAG, "Playing funtone sound");
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(context, alert);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.prepare();
            mMediaPlayer.setLooping(true);
            mMediaPlayer.start();



        } catch (IOException e) {
            e.printStackTrace();
            log(Log.ERROR,TAG, "Failed to play sound. Exception:" + e.getMessage());
        }
    }
    //endregion

    //region Internal helper methods
    private void setVolumeSilentForOutgoingCalls() {
        // if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {//TODO PRECISE RING STATE can't be used so we can't know when the phone is answered. start outgoing in Mute.
        log(Log.INFO,TAG, "android.os.Build.VERSION.SDK_INT : " + String.valueOf(android.os.Build.VERSION.SDK_INT) + " Build.VERSION_CODES.KITKAT = " + Build.VERSION_CODES.KITKAT);
        //    Crashlytics.log(Log.INFO,TAG, "MUTE by button");
        volumeChangeByMCButtons = true;
        verifyAudioManager();
        mVolumeBeforeMute = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        log(Log.INFO,TAG, "MUTE by button , Previous volume: " + String.valueOf(mVolumeBeforeMute));
      //  mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        log(Log.INFO,TAG, "Set Silent , now volume: " + String.valueOf(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
        isMuted = true;

        mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.mute_speaker_anim);//TODO : setImageResource need to be replaced ? memory issue ?
        mSpecialCallMutUnMuteBtn.bringToFront();
        //  }
    }

    private void checkIntent(Intent intent) {
        String action = null;
        if (intent != null)
            action = intent.getAction();
        if (action != null)
            log(Log.INFO,TAG, "Action:" + action);
    }

    private void registerOutgoingReceiver() {
        if (mOutgoingCallReceiver == null) {

            mOutgoingCallReceiver = new OutgoingCallReceiver();
            IntentFilter filter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
            filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
            filter.addAction(StartStandOutServicesFallBackReceiver.ACTION_START_OUTGOING_SERVICE);
            registerReceiver(mOutgoingCallReceiver, filter);
        }
    }

    private void prepareVideoListener() {
        if (mVideoPreparedListener == null)
            mVideoPreparedListener = new OnVideoPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer = new MediaPlayer();
                    mMediaPlayer = mp;
                    mMediaPlayer.setLooping(true);
                    mMediaPlayer.setVolume(1.0f, 1.0f);
                    mMediaPlayer.start();
                    log(Log.INFO,TAG, "prepareVideoListener MUSIC_VOLUME Original" + String.valueOf(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
                }
            };
    }

    private void resumeMusicStreamBackToPrevious() {

        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000); // fix bug: sound in mute , closing call and it sounds for a second in the end.
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                verifyAudioManager();


                try {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME), 0);
                } catch (Exception e) {
                    log(Log.ERROR,TAG, "setStreamVolume  STREAM_MUSIC failed. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
                }
            }
        }.start();
    }

    private void syncWithBuggyIdleState() {

        new Thread() {
            @Override
            public void run() {

                try {
                    Thread.sleep(TIME_TO_SLEEP_AVOIDING_BUGGY_STATE_IDLE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log(Log.INFO,TAG, "syncWithBuggyIdleState");
                setRingingSession(SharedPrefUtils.OUTGOING_RINGING_SESSION, true);
            }
        }.start();
    }
    //endregion

    //region Private classes and listeners
    /**
     * Listener for outgoing call state
     * Responsible for setting and starting special _data on call and restoring previous _data once call is terminated
     */
    private class OutgoingCallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean arrivedFromFallBack = action.equals(StartStandOutServicesFallBackReceiver.ACTION_START_OUTGOING_SERVICE);
              log(Log.INFO,TAG, "outgoingReceiver Action: " + action);
            if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL) || arrivedFromFallBack) {
                String outgoingCallNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                outgoingCallNumber = PhoneNumberUtils.toValidLocalPhoneNumber(outgoingCallNumber);

                if (arrivedFromFallBack)
                    StartStandOutServicesFallBackReceiver.completeWakefulIntent(intent);

                mIncomingOutgoingNumber = outgoingCallNumber;
                log(Log.INFO,TAG, "mInRingingSession=" + isRingingSession(SharedPrefUtils.OUTGOING_RINGING_SESSION) + " outgoingCallNumber=" + outgoingCallNumber);

                boolean isBlocked = MCBlockListUtils.IsMCBlocked(outgoingCallNumber, getApplicationContext());
                if (isBlocked) {
                    _contactName = ContactsUtils.getContactName(getApplicationContext(), outgoingCallNumber);

                    if(_contactName.isEmpty())
                        UI_Utils.callToast("MediaCallz: " + outgoingCallNumber + " is MC BLOCKED !!!  ", Color.RED, Toast.LENGTH_SHORT, getApplicationContext());
                    else
                        UI_Utils.callToast("MediaCallz: " + _contactName + " is MC BLOCKED !!! ", Color.RED, Toast.LENGTH_SHORT, getApplicationContext());


                }

                // Checking if number is in black list
                if (!isBlocked)
                    if (!isRingingSession(SharedPrefUtils.OUTGOING_RINGING_SESSION) && !isRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION) && PhoneNumberUtils.isValidPhoneNumber(outgoingCallNumber)) {

                        try {

                            try { // suppose to solve a bug in samsung that the window shows too fast and make the call screen white. need to let the call screen start before showing our window
                                Thread.sleep(700);
                            }catch (Exception e){
                                e.printStackTrace();
                            }

                            mOutgoingCall=true;

                            String visualMediaFilePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.PROFILE_MEDIA_FILEPATH, outgoingCallNumber);
                            String audioMediaFilePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.FUNTONE_FILEPATH, outgoingCallNumber);

                            File funToneFile = new File(audioMediaFilePath);

                            if(funToneFile.exists()) {
                                funtoneFileExists = true;
                            }

                            backupMusicVolume();

                          //  mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0); // setting max volume for music -5 as it's to high volume


                            setTempMd5ForCallRecord(visualMediaFilePath,audioMediaFilePath);

                            startAudioMediaMC(audioMediaFilePath);
                            startVisualMediaMC(visualMediaFilePath, outgoingCallNumber, funtoneFileExists);


                            MCHistoryUtils.reportMC(
                                    getApplicationContext(),
                                    Constants.MY_ID(getApplicationContext()),
                                    outgoingCallNumber,
                                    visualMediaFilePath,
                                    audioMediaFilePath,
                                    SpecialMediaType.PROFILE_MEDIA);

                            syncWithBuggyIdleState();


                        } catch (Exception e) {
                            e.printStackTrace();
                            log(Log.ERROR,TAG, "CALL_STATE_RINGING failed:" + e.getMessage());
                        }

                        funtoneFileExists = false;

                    }
            }
        }
    }
    //endregion
}
