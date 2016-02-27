package com.services;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.utils.SharedPrefUtils;
import java.io.File;
import java.io.IOException;

import FilesManager.FileManager;
import wei.mark.standout.StandOutWindow;


public class IncomingService extends AbstractStandOutService {

    private boolean mWasSpecialRingTone = false;
    private int mRingVolume;
    private int mOldMediaVolume;
    private boolean mVolumeChangeByService = false;
    private boolean mAlreadyMuted = false;
    private boolean mKeyguardDismissed = false;
    private KeyguardManager mKeyguardManager;
    private KeyguardManager.KeyguardLock mLock;
    private boolean mBugFixPatchForReceiverRegister =true;
    private VolumeButtonReceiver mVolumeButtonReceiver;


    /* Service methods */

    @Override
    public void onCreate() {
        super.onCreate();

        try
        {
            if(mVideoPreparedListener == null)
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

            if(mVolumeButtonReceiver == null)
                mVolumeButtonReceiver = new VolumeButtonReceiver();
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    public IncomingService() {
        super(IncomingService.class.getSimpleName());
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
                        }break;
                    }
                }
            }

        }.start();


        return START_STICKY;

    }

    @Override
    public void createAndAttachView(int id, FrameLayout frame) {
        super.createAndAttachView(id, frame);

        mKeyguardManager = (KeyguardManager)   getSystemService(Activity.KEYGUARD_SERVICE);
        mLock = mKeyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        dismissKeyGuard(true);
    }


    /* AbstractStandOutService methods */

    @Override
    protected void prepareViewForSpecialCall(FileManager.FileType fileType , String mediaFilePath, String callNumber) {
        super.prepareViewForSpecialCall(fileType, mediaFilePath, callNumber);

        if (fileType == FileManager.FileType.VIDEO) {

            mWasSpecialRingTone = true; // Marking that the ring sound will be ours (video stream) and not native

            try {
                mAudioManager.setStreamMute(AudioManager.STREAM_RING, true); // TODO Rony : Replace Deprecated !! Check All places
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
        try
        {
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

    /* Private classes and listeners */


    /**
     * Receiver for volumes button presses
     * Responsible for muting the special call
     */
    private class VolumeButtonReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!volumeChangeByMCButtons) { // this is not a mute by hard button only volume change \ mute by MC buttons , so ignore
                int volumeDuringRun = (Integer) intent.getExtras().get("android.media.EXTRA_VOLUME_STREAM_VALUE");

                Log.i(TAG, "BroadCastFlags: mAlreadyMuted: " + mAlreadyMuted + " mInRingingSession: " + mInRingingSession + " mBugFixPatchForReceiverRegister: " + mBugFixPatchForReceiverRegister);
                if (!mAlreadyMuted && mInRingingSession && (volumeDuringRun != 0) && (volumeDuringRun != 1) && !mBugFixPatchForReceiverRegister/*&& !mVolumeChangeByService*/) {
                    try {
                        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true); // TODO Rony : Replace Deprecated !! Check All places
                        mAlreadyMuted = true;
                        Log.i(TAG, "MUTE STREAM_MUSIC ");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (mVolumeChangeByService)
                    mVolumeChangeByService = false;

                Log.i(TAG, "Exited BroadCast mOldMediaVolume: " + mRingVolume + " volumeDuringRun: " + volumeDuringRun);
            }
            else
                volumeChangeByMCButtons = false;
        }
    };

    /* Assisting methods */

    protected synchronized void syncOnCallStateChange(int state, String incomingNumber) {

        mIncomingOutgoingNumber = incomingNumber;
        // CHECK IF NUMBER BLOCKED OR NOT FOR MC
        if(!checkIfNumberIsMCBlocked(incomingNumber))
            switch(state)
            {
                case TelephonyManager.CALL_STATE_RINGING:

                    if (!mInRingingSession)
                    {
                        try
                        {
                            // Retrieving the ringtone volume
                            mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                            if(mAudioManager!=null)
                                Log.i(TAG, "mAudioManager initialize again" + mAudioManager.toString());
                            else
                                throw new Exception("mAudioManager was returned as null from getSystemService!");

                            mRingVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
                            Log.i(TAG, "mRingVolume Original" + mRingVolume);

                            try
                            {
                                // Backing up the music volume
                                mOldMediaVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

                                // Setting music volume to equal the ringtone volume
                                if (mRingVolume == 0) {
                                    mVolumeChangeByService = true;
                                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0); // ring volume max is 7(also System & Alarm max volume) , Music volume max is 15 (so we want to use full potential of the volume of the music stream)
                                    Log.i(TAG, "STREAM_MUSIC Change : 0");
                                } else {
                                    mVolumeChangeByService = true;
                                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mRingVolume * 2 + 1, 0); // ring volume max is 7(also System & Alarm max volume) , Music volume max is 15 (so we want to use full potential of the volume of the music stream)
                                    Log.i(TAG, "STREAM_MUSIC Change : " + String.valueOf(mRingVolume * 2 + 1));
                                }

                            } catch(Exception e) {
                                e.printStackTrace();
                                Log.e(TAG, "Failed to set stream volume:"+e.getMessage());
                            }

                            String ringtonePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.RINGTONE_FILEPATH, incomingNumber);
                            final File ringtoneFile = new File(ringtonePath);

                            //Check if Mute Was Needed if not return to UnMute.
                            if (ringtoneFile.exists())
                            {
                                mAudioManager.setStreamMute(AudioManager.STREAM_RING, true); // TODO Rony : Replace Deprecated !! Check All places
                                Log.i(TAG, "MUTE STREAM_RING ");

                                Runnable r = new Runnable() {
                                    public void run() {
                                        Log.i(TAG, "startRingtoneSpecialCall Thread");
                                        try {
                                            startAudioSpecialCall(ringtoneFile.getAbsolutePath());

                                        } catch(Exception e) {  e.printStackTrace();  }

                                    }
                                };
                                new Thread(r).start();
                            }
                            else
                            {
                                try {
                                    mAudioManager.setStreamMute(AudioManager.STREAM_RING, false); // TODO Rony : Replace Deprecated !! Check All places
                                    Log.i(TAG, "UNMUTE STREAM_RING ");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            String mediaFilePath
                                    = SharedPrefUtils.getString(getApplicationContext(),
                                    SharedPrefUtils.CALLER_MEDIA_FILEPATH, incomingNumber);

                            startMediaSpecialCall(mediaFilePath, incomingNumber);

                            mInRingingSession = true;
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                            Log.e(TAG, "CALL_STATE_RINGING failed:"+e.getMessage());
                        }
                    }
                    break;

                case TelephonyManager.CALL_STATE_IDLE:
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.i(TAG, "TelephonyManager.CALL_STATE_IDLE");
                    if (mWasSpecialRingTone)
                    {
                        mWasSpecialRingTone = false;
                    }
                    closeSpecialCallWindowAndRingtone();

                    break;

            }

    }



    private void dismissKeyGuard(boolean dismissOrNot) {

        boolean isKeyguardLocked = false;
        if (mKeyguardManager !=null)
            isKeyguardLocked = mKeyguardManager.isKeyguardLocked();


        if(isKeyguardLocked && dismissOrNot)
        {
            mLock.disableKeyguard();
            mKeyguardDismissed =true;
            Log.i(TAG, "Dismiss Keyguard");

        }

        if(mKeyguardDismissed && !dismissOrNot){
            mLock.reenableKeyguard();
            mKeyguardDismissed =false;
            Log.i(TAG, "REenable Keyguard");

        }
        Log.i(TAG, "!!! EnteredDismissedMethod !!! : isKeyGuardLocked: " + isKeyguardLocked + " mKeyguardDismissed: " + mKeyguardDismissed + " dismissOrNot: "+ dismissOrNot);

    }

    private void registerVolumeReceiver() {

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

    }

    private void closeSpecialCallWindowAndRingtone() {

        if  (mInRingingSession) {

            dismissKeyGuard(false);

            Runnable r = new Runnable() {
                public void run() {
                    mInRingingSession = false;
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);// TODO Rony : Replace Deprecated !! Check All places
                    try {
                        if(mVolumeButtonReceiver!=null)
                            unregisterReceiver(mVolumeButtonReceiver);
                    } catch(Exception e) {
                        Log.e(TAG,"UnregisterReceiver failed. Exception:"+ (e.getMessage()!=null? e.getMessage() : e));
                    }

                    mVolumeChangeByService = false;
                    mAlreadyMuted = false;

                    try {
                        Log.i(TAG, "mMediaPlayer.stop(); closeSpecialCallWindowAndRingtone");
                        if(mMediaPlayer!=null)
                            mMediaPlayer.stop();
                    } catch(Exception e) {  e.printStackTrace();  }
                    try {
                        Thread.sleep(2000, 0);
                    } catch (InterruptedException e) { e.printStackTrace(); }
                    try {
                        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false); // TODO Rony : Replace Deprecated !! Check All places
                        Log.i(TAG, "UNMUTE STREAM_MUSIC ");
                    } catch(Exception e) {  e.printStackTrace();  }

                    Log.i(TAG, "UNMUTED." + " mOldMediaVolume: " + mOldMediaVolume + " OldringVolume: " + mRingVolume);
                    try {
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mOldMediaVolume, 0);
                        Log.i(TAG, "STREAM_MUSIC Change : " + String.valueOf(mOldMediaVolume));
                    } catch(Exception e) {  e.printStackTrace();  }
                    try {
                        mAudioManager.setStreamMute(AudioManager.STREAM_RING, false); // TODO Rony : Replace Deprecated !! Check All places
                        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, mRingVolume, 0);
                        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false); // TODO Rony : Replace Deprecated !! Check All places
                        Log.i(TAG, "UNMUTE STREAM_RING ");
                    } catch(Exception e) {  e.printStackTrace();  }
                }
            };

            new Thread(r).start();

            if (!windowCloseActionWasMade) {
                Intent i = new Intent(getApplicationContext(), IncomingService.class);
                i.setAction(StandOutWindow.ACTION_CLOSE);
                startService(i);
                windowCloseActionWasMade=true;
            }
        }

    }

    /* UI methods */

    private void callInfoToast(final String text, final int g) {

        Toast toast = Toast.makeText(getApplicationContext(), text,
                Toast.LENGTH_LONG);
        TextView v = (TextView) toast.getView().findViewById(
                android.R.id.message);
        v.setTextColor(g);
        toast.show();
    }
}
