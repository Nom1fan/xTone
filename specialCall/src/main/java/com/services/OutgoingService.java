package com.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.data_objects.Constants;
import com.receivers.StartStandOutServicesFallBackReceiver;
import com.special.app.R;
import com.utils.MCBlockListUtils;
import com.utils.MCHistoryUtils;
import utils.PhoneNumberUtils;
import com.utils.SharedPrefUtils;

import java.io.File;
import java.io.IOException;

import DataObjects.SpecialMediaType;
import wei.mark.standout.ui.Window;

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
        actionThread(intent);

        if (intent != null) {
            String action = intent.getAction();
            switch (action) {

                case ACTION_PREVIEW: {

                    Log.i(TAG, "ActionPreview Received");
                    mPreviewStart = true;
                    startPreviewWindow(intent);
                }
                default:
                    Log.w(TAG, "Invalid Action: " + action);
            }
        }


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

        Log.i(TAG, "mPreviewStart should mute : " + String.valueOf(mPreviewStart));
        if(!mPreviewStart)
        {
            setVolumeSilentForOutgoingCalls(); // outgoing calls should start in MUTE first
        }
        else
        {
            setVolumeOnForPreview(); // outgoing calls should start in MUTE first
            mPreviewStart = false;
        }
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

                    Log.i(TAG, "TelephonyManager IDLE=0, OFFHOOK=2. STATE WAS:" + state);
                    if (isRingingSession(SharedPrefUtils.OUTGOING_RINGING_SESSION)) {
                        Log.i(TAG, "TelephonyManager inside mInRingingSession IDLE=0, OFFHOOK=2. STATE WAS:" + state);

                        try {
                            closeSpecialCallWindowWithoutRingtone();
                            resumeMusicStreamBackToPrevious();
                            setRingingSession(SharedPrefUtils.OUTGOING_RINGING_SESSION, false);
                        } finally {
                            releaseResources();
                        }
                    }

            }
    }

    @Override
    protected void playSound(Context context, Uri alert) {

        Log.i(TAG, "Playing funtone sound");
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(context, alert);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.prepare();
            mMediaPlayer.setLooping(true);
            mMediaPlayer.start();



        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to play sound. Exception:" + e.getMessage());
        }
    }
    //endregion

    //region Internal helper methods
    protected void startPreviewWindow(Intent intent) {

        Log.i(TAG, "startPreviewWindow");
        String mediaFilePath = intent.getStringExtra(AbstractStandOutService.PREVIEW_VISUAL_MEDIA);
        String funTonePath =   intent.getStringExtra(AbstractStandOutService.PREVIEW_AUDIO);
        String standoutWindowUserTitle = Constants.MY_ID(getApplicationContext());

        File funToneFile = new File(funTonePath);
        startVisualMediaMC(mediaFilePath, standoutWindowUserTitle, funToneFile.exists());
        startAudioSpecialCall(funTonePath);

    }

    private void setVolumeOnForPreview() {

        Log.i(TAG, "setVolumeOnForPreview");
        volumeChangeByMCButtons = true;
        isMuted = false;
        mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.unmute);//TODO : setImageResource need to be replaced ? memory issue ?
        mSpecialCallMutUnMuteBtn.bringToFront();

    }

    private void setVolumeSilentForOutgoingCalls() {
        // if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {//TODO PRECISE RING STATE can't be used so we can't know when the phone is answered. start outgoing in Mute.
        Log.i(TAG, "android.os.Build.VERSION.SDK_INT : " + String.valueOf(android.os.Build.VERSION.SDK_INT) + " Build.VERSION_CODES.KITKAT = " + Build.VERSION_CODES.KITKAT);
        Log.i(TAG, "MUTE by button");
        volumeChangeByMCButtons = true;
        verifyAudioManager();
        mVolumeBeforeMute = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        isMuted = true;

        mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.mute);//TODO : setImageResource need to be replaced ? memory issue ?
        mSpecialCallMutUnMuteBtn.bringToFront();
        //  }
    }

    private void actionThread(Intent intent) {
        String action = null;
        if (intent != null)
            action = intent.getAction();
        if (action != null)
            Log.i(TAG, "Action:" + action);
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
                    mp.setLooping(true);
                    mp.setVolume(1.0f, 1.0f);
                    mp.start();

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
                    Log.e(TAG, "setStreamVolume  STREAM_MUSIC failed. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
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
                Log.i(TAG, "Setting mInRingingSession=true");
                setRingingSession(SharedPrefUtils.OUTGOING_RINGING_SESSION, true);
            }
        }.start();
    }
    //endregion

    //region Private classes and listeners
    /**
     * Listener for outgoing call state
     * Responsible for setting and starting special data on call and restoring previous data once call is terminated
     */
    private class OutgoingCallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean arrivedFromFallBack = action.equals(StartStandOutServicesFallBackReceiver.ACTION_START_OUTGOING_SERVICE);
            Log.i(TAG, "outgoingReceiver Action1: " + action);
            if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL) || arrivedFromFallBack) {
                String outgoingCallNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                mPreviewStart = false;
                outgoingCallNumber = PhoneNumberUtils.toValidLocalPhoneNumber(outgoingCallNumber);

                if (arrivedFromFallBack)
                    StartStandOutServicesFallBackReceiver.completeWakefulIntent(intent);

                mIncomingOutgoingNumber = outgoingCallNumber;
                Log.i(TAG, "outgoingReceiver Action2: " + action);
                Log.i(TAG, "mInRingingSession=" + isRingingSession(SharedPrefUtils.OUTGOING_RINGING_SESSION) + " outgoingCallNumber=" + outgoingCallNumber);

                // Checking if number is in black list
                if (!MCBlockListUtils.IsMCBlocked(outgoingCallNumber, getApplicationContext()))
                    if (!isRingingSession(SharedPrefUtils.OUTGOING_RINGING_SESSION) && !isRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION) && PhoneNumberUtils.isValidPhoneNumber(outgoingCallNumber)) {

                        try {
                            mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

                            String mediaFilePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.PROFILE_MEDIA_FILEPATH, outgoingCallNumber);
                            String funTonePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.FUNTONE_FILEPATH, outgoingCallNumber);

                            File funToneFile = new File(funTonePath);

                            if(funToneFile.exists()) {
                                funtoneFileExists = true;

                            }

                            verifyAudioManager();
                            backupMusicVolume();

                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) - 5, 0); // setting max volume for music -5 as it's to high volume

                            setTempMd5ForCallRecord(mediaFilePath,funTonePath);

                            startVisualMediaMC(mediaFilePath, outgoingCallNumber, funtoneFileExists);
                            startAudioSpecialCall(funTonePath);

                            MCHistoryUtils.reportMC(
                                    getApplicationContext(),
                                    Constants.MY_ID(getApplicationContext()),
                                    outgoingCallNumber,
                                    mediaFilePath,
                                    funTonePath,
                                    SpecialMediaType.PROFILE_MEDIA);

                            syncWithBuggyIdleState();


                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "CALL_STATE_RINGING failed:" + e.getMessage());
                        }

                        funtoneFileExists = false;

                    }
            }
        }
    }
    //endregion
}
