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
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.data.objects.Constants;
import com.data.objects.MediaCallData;
import com.data.objects.PermissionBlockListLevel;
import com.mediacallz.app.R;
import com.receivers.StartStandOutServicesFallBackReceiver;
import com.utils.ContactsUtils;
import com.utils.MCBlockListUtils;
import com.utils.MCHistoryUtils;
import com.utils.MediaCallSessionUtils;
import com.utils.MediaFilesUtils;
import com.utils.SettingsUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.io.File;
import java.io.IOException;

import com.enums.SpecialMediaType;
import com.utils.PhoneNumberUtils;

import wei.mark.standout.ui.Window;

import static com.crashlytics.android.Crashlytics.log;

//import android.telephony.PreciseCallState;


/**
 * Created by Mor on 08/01/2016.
 */
public class OutgoingService extends AbstractStandOutService {

    public static boolean isLive = false;
    private OutgoingCallReceiver mOutgoingCallReceiver;

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

        if (intent != null)
            if (intent.getBooleanExtra(StartStandOutServicesFallBackReceiver.WAKEFUL_INTENT, true))
                StartStandOutServicesFallBackReceiver.completeWakefulIntent(intent);

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
        super.onShow(id, window);  // at last so the volume will return to the previous (since when it was showed) , to make the volume always mute after Unhide move it to the Start of the method.

        // TODO Change this if we find a way to detect call pickup in outgoing calls
        setVolumeSilentForOutgoingCalls(); // outgoing calls should start in MUTE first because we can't detect when the call was answered
        log(Log.INFO, TAG, "setVolumeSilentForOutgoingCalls");

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
        Context context = getApplicationContext();
        if (!MCBlockListUtils.IsMCBlocked(incomingNumber, context)) {
            log(Log.INFO, TAG, "TelephonyManager IDLE=0, OFFHOOK=2. STATE WAS:" + state);
            switch (state) {

                case TelephonyManager.CALL_STATE_IDLE:
                    if (MediaCallSessionUtils.isOutgoingRingingInSession(context)) {
                        log(Log.INFO, TAG, "TelephonyManager inside mInRingingSession IDLE=0, OFFHOOK=2. STATE WAS:" + state);

                        try {
                            closeSpecialCallWindowWithoutRingtone();
                            //   resumeMusicStreamBackToPrevious();
                            MediaCallSessionUtils.setOutgoingRingingSession(context, false);
                        } finally {
                            releaseResources();
                        }
                    }

            }
        }
    }

    @Override
    protected void playSound(Context context, Uri alert) {

        log(Log.INFO, TAG, "Playing funtone sound");
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(context, alert);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);

            verifyAudioManager();
            mVolumeBeforeMute = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            log(Log.INFO, TAG, "MUTE by button , Previous volume: " + String.valueOf(mVolumeBeforeMute));
            mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
            mediaPlayer.start();

        } catch (IOException e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Failed to play sound. Exception:" + e.getMessage());
        }
    }
    //endregion

    //region Internal helper methods
    private void setVolumeSilentForOutgoingCalls() {
        // if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {//TODO PRECISE RING STATE can't be used so we can't know when the phone is answered. start outgoing in Mute.
        log(Log.INFO, TAG, "android.os.Build.VERSION.SDK_INT : " + String.valueOf(android.os.Build.VERSION.SDK_INT) + " Build.VERSION_CODES.KITKAT = " + Build.VERSION_CODES.KITKAT);
        //    Crashlytics.log(Log.INFO,TAG, "MUTE by button");
        volumeChangeByMCButtons = true;
        log(Log.INFO, TAG, "Set Silent , now volume: " + String.valueOf(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
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
            log(Log.INFO, TAG, "Action:" + action);
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
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer = mp;
                    mediaPlayer.setLooping(true);
                    mediaPlayer.setVolume(1.0f, 1.0f);

                    verifyAudioManager();
                    mVolumeBeforeMute = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    log(Log.INFO, TAG, "MUTE by button , Previous volume: " + String.valueOf(mVolumeBeforeMute));
                    mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                    mediaPlayer.start();
                    log(Log.INFO, TAG, "prepareVideoListener MUSIC_VOLUME Original" + String.valueOf(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
                }
            };
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
            log(Log.INFO, TAG, "outgoingReceiver Action: " + action);
            if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL) || arrivedFromFallBack) {

                String outgoingCallNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                outgoingCallNumber = PhoneNumberUtils.toValidLocalPhoneNumber(outgoingCallNumber);

                if (arrivedFromFallBack)
                    StartStandOutServicesFallBackReceiver.completeWakefulIntent(intent);


                mIncomingOutgoingNumber = outgoingCallNumber;
                boolean isOutgoingRingingInSession = MediaCallSessionUtils.isOutgoingRingingInSession(context);
                log(Log.INFO, TAG, "mInRingingSession=" + isOutgoingRingingInSession + " outgoingCallNumber=" + outgoingCallNumber);

                boolean isBlocked = MCBlockListUtils.IsMCBlocked(outgoingCallNumber, getApplicationContext());
                if (isBlocked) {
                    contactName = ContactsUtils.getContactName(getApplicationContext(), outgoingCallNumber);

                    PermissionBlockListLevel permissionLevel = SettingsUtils.getWhoCanMCMe(context);
                    if (!permissionLevel.equals(PermissionBlockListLevel.CONTACTS_ONLY) && permissionLevel.equals(PermissionBlockListLevel.NO_ONE)) {
                        if (contactName.isEmpty()) {
                            UI_Utils.callToast("MediaCallz: " + outgoingCallNumber + " Media Blocked", Color.RED, Toast.LENGTH_SHORT, getApplicationContext());
                        } else {
                            UI_Utils.callToast("MediaCallz: " + contactName + " Media Blocked", Color.RED, Toast.LENGTH_SHORT, getApplicationContext());
                        }
                    }
                }


                // Checking if number is in black list
                if (!isBlocked) {
                    boolean isIncomingRingingInSession = MediaCallSessionUtils.isIncomingRingingInSession(context);

                    if (!isOutgoingRingingInSession && !isIncomingRingingInSession && PhoneNumberUtils.isValidPhoneNumber(outgoingCallNumber)) {
                        backupMusicVolume();
                        try {

                            try { // Supposed to solve a bug in Samsung that the window shows too fast and make the call screen white. need to let the call screen start before showing our window
                                Thread.sleep(700);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            setOutgoingWindowDisplayed(true);

                            MediaCallData mediaCallData = prepareMediaCallData(outgoingCallNumber);

                            setTempMd5ForCallRecord(mediaCallData);

                            if (mediaCallData.doesAudioMediaExist()) {
                                startAudioMediaMC(mediaCallData.getAudioMediaFilePath());
                            }

                            startVisualMediaMC(mediaCallData);

                            MCHistoryUtils.reportMC(
                                    getApplicationContext(),
                                    Constants.MY_ID(context),
                                    outgoingCallNumber,
                                    mediaCallData.getVisualMediaFilePath(),
                                    mediaCallData.getAudioMediaFilePath(),
                                    SpecialMediaType.PROFILE_MEDIA);

                            MediaCallSessionUtils.setOutgoingRingingSession(context, true);

                        } catch (Exception e) {
                            e.printStackTrace();
                            log(Log.ERROR, TAG, "CALL_STATE_RINGING failed:" + e.getMessage());
                        }


                    }
                }
            }
        }
    }

    @NonNull
    protected MediaCallData prepareMediaCallData(String outgoingCallNumber) {
        String visualMediaFilePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.PROFILE_MEDIA_FILEPATH, outgoingCallNumber);
        String audioMediaFilePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.FUNTONE_FILEPATH, outgoingCallNumber);
        boolean visualMediaExists = new File(visualMediaFilePath).exists() && !MediaFilesUtils.isVideoFileCorrupted(visualMediaFilePath, getApplicationContext());
        boolean funToneExists = new File(audioMediaFilePath).exists() && !MediaFilesUtils.isAudioFileCorrupted(audioMediaFilePath, getApplicationContext());

        MediaCallData mediaCallData = new MediaCallData();
        mediaCallData.setPhoneNumber(outgoingCallNumber);
        mediaCallData.setVisualMediaFilePath(visualMediaFilePath);
        mediaCallData.setAudioMediaFilePath(audioMediaFilePath);
        mediaCallData.setDoesVisualMediaExist(visualMediaExists);
        mediaCallData.setDoesAudioMediaExist(funToneExists);
        return mediaCallData;
    }
    //endregion
}
