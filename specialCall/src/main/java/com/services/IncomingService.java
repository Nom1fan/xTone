package com.services;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.FrameLayout;

import com.data_objects.Constants;
import com.receivers.StartStandOutServicesFallBackReceiver;
import com.utils.MCBlockListUtils;
import com.utils.MCHistoryUtils;
import com.utils.SharedPrefUtils;

import java.io.File;
import java.io.IOException;

import DataObjects.SpecialMediaType;
import FilesManager.FileManager;
import wei.mark.standout.StandOutWindow;


public class IncomingService extends AbstractStandOutService {

    public static boolean isLive = false;
    PowerManager.WakeLock wakeLock;
    private boolean mWasSpecialRingTone = false;
    private boolean mVolumeChangeByService = false;
    private boolean mAlreadyMuted = false;
    private boolean mKeyguardDismissed = false;
    private boolean mAnswered = false;
    private KeyguardManager mKeyguardManager;
    private KeyguardManager.KeyguardLock mLock;

    public IncomingService() {
        super(IncomingService.class.getSimpleName());
    }

    //region Service methods
    @Override
    public void onCreate() {
        super.onCreate();

        try {
            if (mVideoPreparedListener == null)
                mVideoPreparedListener = new OnVideoPreparedListener() {

                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.setLooping(true);
                        mp.setVolume(1.0f, 1.0f);
                        mp.start();
                        Log.i(TAG, " Video registerVolumeReceiver");
                        registerVolumeReceiver();
                    }
                };

            // if(mVolumeButtonReceiver == null)
            //     mVolumeButtonReceiver = new VolumeButtonReceiver();
            isLive = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        final Intent intentForThread = intent;

        new Thread() {

            @Override
            public void run() {
                if (intentForThread != null) {
                    String action = intentForThread.getAction();
                    switch (action) {

                        case ACTION_STOP_RING: {
                            stopSound();
                        }
                        break;
                        case ACTION_START: {
                        }
                        break;
                    }
                }
            }

        }.start();

        if (intent != null) {
            String incomingPhoneNumber = intent.getStringExtra(StartStandOutServicesFallBackReceiver.INCOMING_PHONE_NUMBER_KEY);
            Log.i(TAG, "intent.getStringExtra.IncomingPhoneNumberFromBroadcastReceiver mincomingnumber:" + incomingPhoneNumber);

            // do you start from FallBackReceiver ??
            if (incomingPhoneNumber != null && !incomingPhoneNumber.isEmpty()) {
                Log.i(TAG, "syncOnCallStateChange From FALLBACK mincomingnumber:" + incomingPhoneNumber);

                StartStandOutServicesFallBackReceiver.completeWakefulIntent(intent);

                syncOnCallStateChange(TelephonyManager.CALL_STATE_RINGING, incomingPhoneNumber);
            }

        }

        return START_STICKY;

    }

    @Override
    public void onDestroy() {

        isLive = false;
        super.onDestroy();
    }
    //endregion

    //region AbstractStandOutService methods
    @Override
    public void createAndAttachView(int id, FrameLayout frame) {
        super.createAndAttachView(id, frame);

        mKeyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
        mLock = mKeyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        dismissKeyGuard(true);
    }

    @Override
    protected void prepareViewForSpecialCall(FileManager.FileType fileType, String mediaFilePath, String callNumber) {
        super.prepareViewForSpecialCall(fileType, mediaFilePath, callNumber);

        if (fileType == FileManager.FileType.VIDEO) {

            mWasSpecialRingTone = true; // Marking that the ring sound will be ours (video stream) and not native

            try {
                disableRingStream();
                Log.i(TAG, "MUTE STREAM_RING ");
                Log.i(TAG, "VIDEO file detected MUTE Ring");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void playSound(Context context, Uri alert) {

        Log.i(TAG, "Playing ringtone sound");
        mMediaPlayer = new MediaPlayer();
        try {
            try {
                mMediaPlayer.setDataSource(context, alert);
            } catch (IOException e) {
                e.printStackTrace();
            }
            final AudioManager audioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.prepare();
                mMediaPlayer.setLooping(true);
                mMediaPlayer.start();

                Log.i(TAG, " Ringtone registerVolumeReceiver");
                registerVolumeReceiver();

            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to play sound. Exception:" + e.getMessage());
        }
    }

    @Override
    protected synchronized void syncOnCallStateChange(int state, String incomingNumber) {

        mIncomingOutgoingNumber = incomingNumber;
        // Checking if number is in black list
        if (!MCBlockListUtils.IsMCBlocked(incomingNumber, getApplicationContext()) || (isRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION)))
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:

                    if (!isRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION)) {
                        try {
                            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My wakelock");
                            wakeLock.acquire();

                            String mediaFilePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.CALLER_MEDIA_FILEPATH, incomingNumber);
                            String ringtonePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.RINGTONE_FILEPATH, incomingNumber);
                            File mediaFile = new File(mediaFilePath);
                            final File ringtoneFile = new File(ringtonePath);

                            mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

                            backupRingVolume();
                            verifyAudioManager();

                            try {

                                backupMusicVolume();

                                int ringVolume = SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME);
                                boolean isSilent = (ringVolume == 0);

                                // Setting music volume to equal the ringtone volume
                                if (isSilent) {

                                    mVolumeChangeByService = true;
                                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0); // ring volume max is 7(also System & Alarm max volume) ,
                                    // Music volume max is 15 (so we want to use full potential of the volume of the music stream)
                                    Log.i(TAG, "STREAM_MUSIC Change : 0");

                                } else {

                                    mVolumeChangeByService = true;
                                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, ringVolume * 2 + 1, 0); // ring volume max is 7(also System & Alarm max volume) ,
                                    // Music volume max is 15 (so we want to use full potential of the volume of the music stream)
                                    Log.i(TAG, "STREAM_MUSIC Change : " + (ringVolume * 2 + 1));
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e(TAG, "Failed to set stream volume:" + e.getMessage());
                            }

                            //Check if Mute Was Needed if not return to UnMute.
                            if (ringtoneFile.exists()) {
                                disableRingStream();

                                Runnable r = new Runnable() {
                                    public void run() {
                                        Log.i(TAG, "startRingtoneSpecialCall Thread");
                                        try {
                                            startAudioSpecialCall(ringtoneFile.getAbsolutePath());

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                };
                                new Thread(r).start();
                            } else
                                enableRingStream();

                            setTempMd5ForCallRecord(mediaFilePath,ringtonePath);

                            startVisualMediaMC(mediaFilePath, incomingNumber, ringtoneFile.exists());
                            setRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION, true);

                            MCHistoryUtils.reportMC(
                                    getApplicationContext(),
                                    incomingNumber,
                                    Constants.MY_ID(getApplicationContext()),
                                    mediaFile.exists() ? mediaFilePath : null,
                                    ringtoneFile.exists() ? ringtonePath : null,
                                    SpecialMediaType.CALLER_MEDIA);

                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "CALL_STATE_RINGING failed:" + e.getMessage());
                        }
                    }
                    break;
                // TODO RONY Iteratively  inside enableRingStream() and   i.setAction(StandOutWindow.ACTION_CLOSE_ALL); will be before this method enableRingStream iteratively logic
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (isRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION)) {
                        Log.i(TAG, "mAnswered = true");
                        mAnswered = true;
                        closeSpecialCallWindowAndRingtone();
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.i(TAG, "TelephonyManager.CALL_STATE_IDLE");
                    if (mWasSpecialRingTone) {
                        mWasSpecialRingTone = false;
                    }
                    closeSpecialCallWindowAndRingtone();
                    if (mAnswered) {

                        Runnable r = new Runnable() {
                            public void run() {
                                Log.i(TAG, "mAnswered = false");
                                try {
                                    Thread.sleep(2000, 0);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                enableRingStream();
                                mAnswered = false;
                            }
                        };
                        new Thread(r).start();
                    }
            }
    }
    //endregion

    //region Internal helper methods
    private void enableRingStream() {

        try {
            mAudioManager.setStreamMute(AudioManager.STREAM_RING, false);
            Log.e(TAG, "mAudioManager.setStreamMute(AudioManager.STREAM_RING, false);");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed mAudioManager.setStreamMute(AudioManager.STREAM_RING, false); error:" + e.getMessage());
        }
        try {
            if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME) != mAudioManager.getStreamVolume(AudioManager.STREAM_RING)) {  // resuming previous Ring Volume
                mAudioManager.setStreamVolume(AudioManager.STREAM_RING, SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME), 0);
                Log.e(TAG, "mAudioManager.setStreamVolume(AudioManager.STREAM_RING, : " + String.valueOf(SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed  mAudioManager.setStreamVolume(AudioManager.STREAM_RING); error:" + e.getMessage());
        }
    }

    private void disableRingStream() {
        try {
            mAudioManager.setStreamMute(AudioManager.STREAM_RING, true);
            Log.e(TAG, "mAudioManager.setStreamMute(AudioManager.STREAM_RING, true);");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed mAudioManager.setStreamMute(AudioManager.STREAM_RING, true); error:" + e.getMessage());
        }
        try {
            mAudioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
            Log.e(TAG, "mAudioManager.setStreamVolume(AudioManager.STREAM_RING, : 0");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed  mAudioManager.setStreamVolume(AudioManager.STREAM_RING); error:" + e.getMessage());
        }
    }

    private void dismissKeyGuard(boolean dismissOrNot) {

        boolean isKeyguardLocked = false;
        if (mKeyguardManager != null)
            isKeyguardLocked = mKeyguardManager.isKeyguardLocked();


        if (isKeyguardLocked && dismissOrNot) {
            mLock.disableKeyguard();
            mKeyguardDismissed = true;
            Log.i(TAG, "Dismiss Keyguard");

        }

        if (mKeyguardDismissed && !dismissOrNot) {
            mLock.reenableKeyguard();
            mKeyguardDismissed = false;
            Log.i(TAG, "REenable Keyguard");

        }
        Log.i(TAG, "!!! EnteredDismissedMethod !!! : isKeyGuardLocked: " + isKeyguardLocked + " mKeyguardDismissed: " + mKeyguardDismissed + " dismissOrNot: " + dismissOrNot);

    }

    private void registerVolumeReceiver() {
/*   // TODO UNCOMMENT IT IF WE NEED TO MUTE THE STREAM THROUGH THE HARD VOLUME BUTTONS  (WE COMMENTED THIS BECAUSE IT HAD SOME ISSUES WITH THE MC VOLUME BUTTON) , IF UNCOMMENT QA THE SHIT OUT OF IT.
        Runnable r = new Runnable() {
            public void run() {

                mBugFixPatchForReceiverRegister =true; // when registered sometimes it enters the receiver and causes an independent mute.
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.media.VOLUME_CHANGED_ACTION");

                registerReceiver(mVolumeButtonReceiver, filter);
                Log.i(TAG, "registerReceiver VolumeButtonReceiver Finished register");

                try {
                    Thread.sleep(1000,0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mBugFixPatchForReceiverRegister =false; // when registered sometimes it enters the receiver and causes an independent mute.
            }
        };

        new Thread(r).start();
*/
    }

    private void closeSpecialCallWindowAndRingtone() {

        if (isRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION)) {

            try {
                wakeLock.release();
                dismissKeyGuard(false);
                enableRingStream();

                Runnable r = new Runnable() {
                    public void run() {
                        setRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION, false);

                 /*   try {
                        if(mVolumeButtonReceiver!=null)
                            unregisterReceiver(mVolumeButtonReceiver);
                    } catch(Exception e) {
                        Log.e(TAG,"UnregisterReceiver failed. Exception:"+ (e.getMessage()!=null? e.getMessage() : e));
                    }*/

                        mVolumeChangeByService = false;
                        mAlreadyMuted = false;

                        try {
                            Log.i(TAG, "mMediaPlayer.stop(); closeSpecialCallWindowAndRingtone");
                            if (mMediaPlayer != null)
                                mMediaPlayer.stop();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            Thread.sleep(2000, 0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {

                            if (mAudioManager == null)
                                mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME), 0);
                            Log.i(TAG, "UNMUTE STREAM_MUSIC ");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Log.i(TAG, "UNMUTED." + " mOldMediaVolume: " + SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME) + " OldringVolume: " + SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME));
                        try {
                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME), 0);
                            Log.i(TAG, "STREAM_MUSIC Change : " + String.valueOf(SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            enableRingStream();
                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME), 0);
                            Log.i(TAG, "UNMUTE STREAM_RING ");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                };

                new Thread(r).start();


                try {
                    if (mAudioManager == null)
                        mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Intent i = new Intent(getApplicationContext(), IncomingService.class);
                i.setAction(StandOutWindow.ACTION_CLOSE_ALL);
                startService(i);
            } finally {


                releaseResources();

            }
        }
    }
    //endregion

    //region Private classes and listeners
    /**
     * Receiver for volumes button presses
     * Responsible for muting the special call
     */
    private class VolumeButtonReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (!volumeChangeByMCButtons) { // This is not a mute by hard button only volume change / mute by MC buttons , so ignore
                int volumeDuringRun = (Integer) intent.getExtras().get("android.media.EXTRA_VOLUME_STREAM_VALUE");

                boolean mBugFixPatchForReceiverRegister = true;
                Log.i(TAG, "BroadCastFlags: mAlreadyMuted: " + mAlreadyMuted + " mInRingingSession: " + isRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION) + " mBugFixPatchForReceiverRegister: " + mBugFixPatchForReceiverRegister);

                if (mVolumeChangeByService)
                    mVolumeChangeByService = false;

                Log.i(TAG, "Exited BroadCast mOldMediaVolume: " + SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME) + " volumeDuringRun: " + volumeDuringRun);
            } else
                volumeChangeByMCButtons = false;
        }
    }
    //endregion


}
