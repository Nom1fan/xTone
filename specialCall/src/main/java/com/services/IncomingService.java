package com.services;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.data_objects.Constants;
import com.data_objects.PermissionBlockListLevel;
import com.mediacallz.app.R;
import com.receivers.StartStandOutServicesFallBackReceiver;
import com.utils.ContactsUtils;
import com.utils.MCBlockListUtils;
import com.utils.MCHistoryUtils;
import com.utils.MediaFilesUtils;
import com.utils.NotificationUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import DataObjects.SpecialMediaType;
import FilesManager.FileManager;
import utils.PhoneNumberUtils;
import wei.mark.standout.StandOutWindow;

import static com.crashlytics.android.Crashlytics.log;


public class IncomingService extends AbstractStandOutService {

    public static boolean isLive = false;
    private boolean mWasSpecialRingTone = false;
    private boolean mVolumeChangeByService = false;
    private boolean mAlreadyMuted = false;
    private boolean mKeyguardDismissed = false;
    private boolean mAnswered = false;
    private KeyguardManager mKeyguardManager;
    private KeyguardManager.KeyguardLock mLock;
    public static final String ACTION_START_FOREGROUND = "com.services.IncomingService.ACTION_START_FOREGROUND";
    public static final String ACTION_STOP_FOREGROUND = "com.services.IncomingService.ACTION_STOP_FOREGROUND";


    public IncomingService() {
        super(IncomingService.class.getSimpleName());
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

        prepareVideoListener();
        checkIntent(intent);
        actionThread(intent);

        if (intent != null) {
            if (intent.getAction().equals(
                    ACTION_STOP_FOREGROUND)) {
                log(Log.INFO,TAG, "Received Stop Foreground Intent");
                stopForeground(true);
            } else if (intent.getAction().equals(
                    ACTION_START_FOREGROUND)) {
                log(Log.INFO,TAG, "Received Start Foreground Intent ");
                isForegroundAndAlarmNeeded(); // check if the Device has Strict Memory Manager like SPCM that always kills us on lowestscore package!
            }
        }

        //check if the fallback receiver sent the intent and the service is down so we should use it.
        checkIfItsFallBackReceiverIntent(intent);

        // check if the Device has Strict Memory Manager like SPCM that always kills us on lowestscore package!
        isForegroundAndAlarmNeeded();

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
    protected void prepareViewForSpecialCall(FileManager.FileType fileType, String mediaFilePath) {
        super.prepareViewForSpecialCall(fileType, mediaFilePath);

        if (fileType == FileManager.FileType.VIDEO) {

            mWasSpecialRingTone = true; // Marking that the ring sound will be ours (video stream) and not native

            try {
                disableRingStream();
                log(Log.INFO,TAG, "MUTE STREAM_RING ");
                log(Log.INFO,TAG, "VIDEO file detected MUTE Ring");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void playSound(Context context, Uri alert) {

        log(Log.INFO,TAG, "Playing ringtone sound");
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

                //Crashlytics.log(Log.INFO,TAG, " Ringtone registerVolumeReceiver");
                registerVolumeReceiver();

            }
        } catch (IOException e) {
            e.printStackTrace();
            log(Log.ERROR,TAG, "Failed to play sound. Exception:" + e.getMessage());
        }
    }

    @Override
    protected synchronized void syncOnCallStateChange(int state, String incomingNumber) {

        incomingNumber = PhoneNumberUtils.toValidLocalPhoneNumber(incomingNumber);
        log(Log.INFO,TAG,"before incoming phone number : " + incomingNumber);
        mIncomingOutgoingNumber = incomingNumber;
        // Checking if number is in black list
        log(Log.INFO,TAG, " mInRingingSession: " + isRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION));
        boolean isBlocked = MCBlockListUtils.IsMCBlocked(incomingNumber, getApplicationContext());
        if (isBlocked) {

            if (state == TelephonyManager.CALL_STATE_RINGING) {
                UI_Utils.dismissAllStandOutWindows(getApplicationContext());
                _contactName = ContactsUtils.getContactName(getApplicationContext(), incomingNumber);

                String permissionLevel = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.RADIO_BUTTON_SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME);
                if (permissionLevel != PermissionBlockListLevel.CONTACTS_ONLY && permissionLevel != PermissionBlockListLevel.NO_ONE) {
                    if (_contactName.isEmpty())
                        UI_Utils.callToast("MediaCallz: " + incomingNumber + " Media Blocked", Color.RED, Toast.LENGTH_SHORT, getApplicationContext());
                    else
                        UI_Utils.callToast("MediaCallz: " + _contactName + " Media Blocked", Color.RED, Toast.LENGTH_SHORT, getApplicationContext());
                }
            }

        }
        if (!isBlocked || (isRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION)))
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:

                    log(Log.INFO,TAG,"CALL_STATE_RINGING " + incomingNumber);
                    if (!isRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION) && !isRingingSession(SharedPrefUtils.OUTGOING_RINGING_SESSION) && PhoneNumberUtils.isValidPhoneNumber(incomingNumber) && (mAnswered == false)) {
                        try {
                            UI_Utils.dismissAllStandOutWindows(getApplicationContext());
                            SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.INCOMING_WINDOW_SESSION,true);
                            setRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION, true); // TODO placed here to fix a bug that sometimes it get entered twice (second time by the fallback receiver when we answer very quick) , is this a good place for it i don't know :/

                            mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                            backupRingSettings();
                            backupMusicVolume();

                            String mediaFilePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.CALLER_MEDIA_FILEPATH, incomingNumber);
                            String ringtonePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.RINGTONE_FILEPATH, incomingNumber);

                            boolean ringtoneExists = new File(ringtonePath).exists() && !MediaFilesUtils.isAudioFileCorrupted(ringtonePath,getApplicationContext());
                            boolean visualMediaExists = new File(mediaFilePath).exists() && !MediaFilesUtils.isVideoFileCorrupted(mediaFilePath,getApplicationContext());

                            if (ringtoneExists || visualMediaExists){

                                if (SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.ASK_BEFORE_MEDIA_SHOW)
                                        && !SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.ASK_BEFORE_MEDIA_SHOW_FOR_STANDOUT)) {
                                    runAskBeforeShowMedia(incomingNumber,ringtoneExists,visualMediaExists);
                                } else {
                                    runIncomingMCMedia(incomingNumber,ringtoneExists,visualMediaExists);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            log(Log.ERROR,TAG, "CALL_STATE_RINGING failed:" + e.getMessage());
                        }
                    }

                    break;
                // TODO RONY Iteratively  inside enableRingStream() and   i.setAction(StandOutWindow.ACTION_CLOSE_ALL); will be before this method enableRingStream iteratively logic
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (isRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION)) {
                        log(Log.INFO,TAG, "mAnswered = true");
                        mAnswered = true;
                        stopVibrator();
                        closeSpecialCallWindowAndRingtone();
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    log(Log.INFO,TAG, "TelephonyManager.CALL_STATE_IDLE");
                    if (mWasSpecialRingTone) {
                        mWasSpecialRingTone = false;
                    }
                    stopVibrator();

                    closeSpecialCallWindowAndRingtone();
                    if (mAnswered) {

                        Runnable r = new Runnable() {
                            public void run() {
                                log(Log.INFO,TAG, "mAnswered = false");
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

    private void runAskBeforeShowMedia(String incomingNumber,boolean ringtoneExists,boolean visualMediaExists) {
        SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.ASK_BEFORE_MEDIA_SHOW_FOR_STANDOUT , true);
        SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.DISABLE_VOLUME_BUTTONS,true);

        _contactName = ContactsUtils.getContactName(getApplicationContext(), incomingNumber);
        mContactTitleOnWindow = (!_contactName.equals("") ? _contactName + " " + incomingNumber : incomingNumber);
        Random r = new Random();
        int randomWindowId = r.nextInt(Integer.MAX_VALUE);  // fixing a bug: when the same ID the window isn't released good enough so we need to make a different window in the mean time
        prepareAskBeforeShowViewForSpecialCall(incomingNumber,ringtoneExists,visualMediaExists);
        Intent i = new Intent(this, this.getClass());
        i.putExtra("id", randomWindowId);
        i.setAction(StandOutWindow.ACTION_SHOW);
        startService(i);
    }

    private void prepareAskBeforeShowViewForSpecialCall(final String callNumber , final boolean ringtoneExists , final boolean visualMediaExists) {
        log(Log.INFO,TAG, "Preparing SpecialCall view");

        // Attempting to induce garbage collection
        System.gc();

        prepareRelativeLayout();

        mSpecialCallView = new ImageView(this);
        mSpecialCallView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        //     ((ImageView)mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_XY); STRECTH IMAGE ON FULL SCREEN <<< NOT SURE IT's GOOD !!!!!
        ((ImageView) mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_CENTER); // <<  just place the image Center of Window and fit it with ratio

        ((ImageView) mSpecialCallView).setImageResource(R.drawable.color_mc_anim);

        mSpecialCallView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                log(Log.INFO,TAG, "Asked to show media ! and he Said YES !! ");
               // close(randomWindowId);
                closeAll();
                runIncomingMCMedia(callNumber,ringtoneExists,visualMediaExists);
            }
        });


        mRelativeLayout.addView(mSpecialCallView);
    }



    private void runIncomingMCMedia(String incomingNumber,boolean ringtoneExists,boolean visualMediaExists) {

        SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.ASK_BEFORE_MEDIA_SHOW_FOR_STANDOUT , false);
        SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.INCOMING_WINDOW_SESSION,true);

        mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        backupRingSettings();
        backupMusicVolume();

        showFirstTime = true;

        String mediaFilePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.CALLER_MEDIA_FILEPATH, incomingNumber);
        String ringtonePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.RINGTONE_FILEPATH, incomingNumber);
        File mediaFile = new File(mediaFilePath);
        final File ringtoneFile = new File(ringtonePath);

        try{
            setupStandOutWindowMusicVolumeLogic();

        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR,TAG, "Failed to set stream volume:" + e.getMessage());
        }

        //Check if Mute Was Needed if not return to UnMute.
        if (ringtoneExists) {

            disableRingStream();
            SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.DISABLE_VOLUME_BUTTONS, false);
            Runnable r = new Runnable() {
                public void run() {
                    Log.d(TAG, "startRingtoneSpecialCall Thread");
                    try {
                        startAudioMediaMC(ringtoneFile.getAbsolutePath());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            new Thread(r).start();
        } else {
            SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.DISABLE_VOLUME_BUTTONS,true);
            Log.i(TAG,"No Ringtone !!");
            ringtoneExists = false; // don't show volume buttons
        }

        setTempMd5ForCallRecord(mediaFilePath, ringtonePath);

        startVisualMediaMC(mediaFilePath, incomingNumber,ringtoneExists,visualMediaExists);


        MCHistoryUtils.reportMC(
                getApplicationContext(),
                incomingNumber,
                Constants.MY_ID(getApplicationContext()),
                visualMediaExists ? mediaFilePath : null,
                ringtoneExists ? ringtonePath : null,
                SpecialMediaType.CALLER_MEDIA);


    }

    private void setupStandOutWindowMusicVolumeLogic() {

        int ringVolume = SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME);

        // TODO maybe add GetRingerMode rule just in case volume is above 0 and mode is silent... never saw that but maybe we should cover this scenario if there is some weird devices :/ for now no need... need to check
        // Setting music volume to equal the ringtone volume
        mVolumeChangeByService = true;
        // Get the current ringer volume as a percentage of the max ringer volume.
        int maxRingerVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        Log.d(TAG, "maxRingerVolume: " +maxRingerVolume);
        double proportion = ringVolume/(double)maxRingerVolume;

        // Calculate a desired music volume as that same percentage of the max music volume.
        int maxMusicVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "maxMusicVolume: " +maxMusicVolume);
        int desiredMusicVolume = (int)(proportion * maxMusicVolume);

        // Set the music stream volume.
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, desiredMusicVolume, 0 /*flags*/);

        Log.d(TAG, "STREAM_MUSIC Change : " + (desiredMusicVolume));

    }
    //endregion

    //region Internal helper methods
    private void isForegroundAndAlarmNeeded() {
      boolean shouldStartForeground = false;//true;//TODO remove it when we know how to keep it working against SPCM or other memory services//SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.GENERAL,SharedPrefUtils.STRICT_MEMORY_MANAGER_DEVICES);
        log(Log.INFO,TAG, "shouldStartForeground : " + String.valueOf(shouldStartForeground));
        if (shouldStartForeground)
        {
            //TODO ForeGroundService needed & Alarm Needed or this is solved after memory leakage solved????

            startForegoundService();

            setAlarm(this);
        }
    }

    public void startForegoundService() {
        startForeground(NotificationUtils.FOREGROUND_NOTIFICATION_ID, NotificationUtils.getCompatNotification(getApplicationContext()));
        log(Log.INFO,TAG,"STARTED FOREGROUND");
    }

    private void checkIfItsFallBackReceiverIntent(Intent intent) {
        if (intent != null) {

            //release wake lock from fallbackreceiver
          if (intent.getBooleanExtra(StartStandOutServicesFallBackReceiver.WAKEFUL_INTENT,true))
                    StartStandOutServicesFallBackReceiver.completeWakefulIntent(intent);

            String incomingPhoneNumber = intent.getStringExtra(StartStandOutServicesFallBackReceiver.INCOMING_PHONE_NUMBER_KEY);
            log(Log.INFO,TAG, "FallBackReceiver Gives incoming number:" + incomingPhoneNumber);

            // do you start from FallBackReceiver ??
            if (incomingPhoneNumber != null && !incomingPhoneNumber.isEmpty()) {
                log(Log.INFO,TAG, "sending fallbackReceiver incoming number to SyncOnCallState: " + incomingPhoneNumber);
                //Initiating syncOnCallStateChange
                syncOnCallStateChange(TelephonyManager.CALL_STATE_RINGING, incomingPhoneNumber);
            }
        }

    }

    private void checkIntent(Intent intent) {
        String action = null;
        if (intent != null)
            action = intent.getAction();
        if (action != null)
            log(Log.INFO,TAG, "Action:" + action);
    }

    private void actionThread(Intent intent) {
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

    }

    private void prepareVideoListener() {
        try {
            if (mVideoPreparedListener == null)
                mVideoPreparedListener = new OnVideoPreparedListener() {

                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mMediaPlayer = new MediaPlayer();
                        mMediaPlayer = mp;
                        mMediaPlayer.setLooping(true);
                        mMediaPlayer.setVolume(1.0f, 1.0f);
                        mMediaPlayer.start();
                        log(Log.INFO,TAG, " Video registerVolumeReceiver");
                        registerVolumeReceiver();
                    }
                };

            // if(mVolumeButtonReceiver == null)
            //     mVolumeButtonReceiver = new VolumeButtonReceiver();

        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR,TAG, e.getMessage());
        }
    }

    private void returnToPreviousRingerMode(){

        try {
            verifyAudioManager();
          if (mAudioManager.getRingerMode() != SharedPrefUtils.getInt(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.RINGER_MODE)){
              log(Log.INFO,TAG, "Set Ringer Mode back To Normal:" + SharedPrefUtils.getInt(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.RINGER_MODE) + " current RingerMode: " + mAudioManager.getRingerMode());
              mAudioManager.setRingerMode(SharedPrefUtils.getInt(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.RINGER_MODE));
          }
        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR,TAG, "Set Ringer Mode back To Normal error:" + e.getMessage());
        }


    }

    private void enableRingStream() {

        try {
            verifyAudioManager();
           if (SharedPrefUtils.getInt(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.RINGER_MODE) == AudioManager.RINGER_MODE_NORMAL)
            mAudioManager.setStreamMute(AudioManager.STREAM_RING, false);
            log(Log.INFO,TAG, "mAudioManager.setStreamMute(AudioManager.STREAM_RING, false);");
        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR,TAG, "Failed mAudioManager.setStreamMute(AudioManager.STREAM_RING, false); error:" + e.getMessage());
        }
        try {
            if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME) != mAudioManager.getStreamVolume(AudioManager.STREAM_RING)
                    && SharedPrefUtils.getInt(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.RINGER_MODE) == AudioManager.RINGER_MODE_NORMAL) {  // resuming previous Ring Volume
                log(Log.INFO,TAG, "AudioManager.STREAM_RING when ringermode is : " + String.valueOf(SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RINGER_MODE)));
                mAudioManager.setStreamVolume(AudioManager.STREAM_RING, SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME), 0);
                log(Log.INFO,TAG, "mAudioManager.setStreamVolume(AudioManager.STREAM_RING, : " + String.valueOf(SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR,TAG, "Failed  mAudioManager.setStreamVolume(AudioManager.STREAM_RING); error:" + e.getMessage());
        }
    }

    private void disableRingStream() {

        // check if the Device has Strict Ringing Capabilities that hard to be silent like in LG G4
        if (SharedPrefUtils.getBoolean(getApplicationContext(),SharedPrefUtils.GENERAL,SharedPrefUtils.STRICT_RINGING_CAPABILITIES_DEVICES)) {
            unlockMusicStreamDuringRinging();
            correlateVibrateSettings();
        }

        try {
            verifyAudioManager();
            if (SharedPrefUtils.getInt(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.RINGER_MODE) == AudioManager.RINGER_MODE_NORMAL)
            mAudioManager.setStreamMute(AudioManager.STREAM_RING, true);
            log(Log.INFO,TAG, "mAudioManager.setStreamMute(AudioManager.STREAM_RING, true);");
        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR,TAG, "Failed mAudioManager.setStreamMute(AudioManager.STREAM_RING, true); error:" + e.getMessage());
        }
        try {
            if (SharedPrefUtils.getInt(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.RINGER_MODE) == AudioManager.RINGER_MODE_NORMAL)
            mAudioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
            log(Log.INFO,TAG, "mAudioManager.setStreamVolume(AudioManager.STREAM_RING, : 0");
        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR,TAG, "Failed  mAudioManager.setStreamVolume(AudioManager.STREAM_RING); error:" + e.getMessage());
        }
    }

    /**
     *  in certain devices , like LG G4 G3 ... the music stream is locked during ringing. this code unlocks it.
     *  in other devices it doesn't hurt anything
     */
    private void unlockMusicStreamDuringRinging() {

            verifyAudioManager();
            log(Log.INFO,TAG, "unlockMusicStreamDuringRinging , getRingerMode: " +mAudioManager.getRingerMode());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            log(Log.INFO,TAG, "unlockMusicStreamDuringRinging , Setting To Silent");
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);  // SOLUTION For LG G4 that needs another motivation to be silent (if removed the audio isn't heared in LG G4 you need to press the volume hard keys to silent manually , this fixes it)
            log(Log.INFO,TAG, "unlockMusicStreamDuringRinging , getRingerMode: " +mAudioManager.getRingerMode());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log(Log.INFO,TAG, "unlockMusicStreamDuringRinging , Setting Back To Normal");
            mAudioManager.setRingerMode(SharedPrefUtils.getInt(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.RINGER_MODE));  // SOLUTION For LG G4 that needs another motivation to be silent (if removed the audio isn't heared in LG G4 you need to press the volume hard keys to silent manually , this fixes it)
            log(Log.INFO,TAG, "unlockMusicStreamDuringRinging , getRingerMode: " +mAudioManager.getRingerMode());
    }

    private void correlateVibrateSettings() {

        //get vibrate settings and vibrate if needed.
        boolean isVibrateOn = Settings.System.getInt(this.getApplicationContext().getContentResolver(),"vibrate_when_ringing", 0) != 0;
        log(Log.INFO,TAG,"isVibrateOn: " + isVibrateOn);
        if ((mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL && isVibrateOn) || mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)
            vibrateIfNeeded();

    }

    private void vibrateIfNeeded() {

       try {
           vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

           //Set the pattern, like vibrate for 300 milliseconds and then stop for 200 ms, then
           //vibrate for 300 milliseconds and then stop for 500 ms and repeat the same style. You can change the pattern and
           // test the result for better clarity.
           long pattern[] = {300, 1800, 800, 1800, 800};
           //start vibration with repeated count, use -1 if you don't want to repeat the vibration
           vibrator.vibrate(pattern, 1);
       }catch (Exception e) {
           e.printStackTrace();
       }


   }




    private void dismissKeyGuard(boolean dismissOrNot) {

        boolean isKeyguardLocked = false;
        if (mKeyguardManager != null)
            isKeyguardLocked = mKeyguardManager.isKeyguardLocked();


        if (isKeyguardLocked && dismissOrNot) {
            mLock.disableKeyguard();
            mKeyguardDismissed = true;
            log(Log.INFO,TAG, "Dismiss Keyguard");

        }

        if (mKeyguardDismissed && !dismissOrNot) {
            mLock.reenableKeyguard();
            mKeyguardDismissed = false;
            log(Log.INFO,TAG, "REenable Keyguard");

        }
        log(Log.INFO,TAG, "!!! EnteredDismissedMethod !!! : isKeyGuardLocked: " + isKeyguardLocked + " mKeyguardDismissed: " + mKeyguardDismissed + " dismissOrNot: " + dismissOrNot);

    }

    private void registerVolumeReceiver() {
/*   // TODO UNCOMMENT IT IF WE NEED TO MUTE THE STREAM THROUGH THE HARD VOLUME BUTTONS  (WE COMMENTED THIS BECAUSE IT HAD SOME ISSUES WITH THE MC VOLUME BUTTON) , IF UNCOMMENT QA THE SHIT OUT OF IT.
        Runnable r = new Runnable() {
            public void run() {

                mBugFixPatchForReceiverRegister =true; // when registered sometimes it enters the receiver and causes an independent mute.
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.media.VOLUME_CHANGED_ACTION");

                registerReceiver(mVolumeButtonReceiver, filter);
                Crashlytics.log(Log.INFO,TAG, "registerReceiver VolumeButtonReceiver Finished register");

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

                dismissKeyGuard(false);
                enableRingStream();

                try {
                    log(Log.INFO,TAG, "mMediaPlayer.stop(); closeSpecialCallWindowAndRingtone");
                    if (mMediaPlayer != null && mMediaPlayer.isPlaying())
                    {
                        mMediaPlayer.setVolume(0, 0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }



                try {
                    returnToPreviousRingerMode();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Runnable r = new Runnable() {
                    public void run() {
                        setRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION, false);

                 /*   try {
                        if(mVolumeButtonReceiver!=null)
                            unregisterReceiver(mVolumeButtonReceiver);
                    } catch(Exception e) {
                        Crashlytics.log(Log.ERROR,TAG,"UnregisterReceiver failed. Exception:"+ (e.getMessage()!=null? e.getMessage() : e));
                    }*/

                        mVolumeChangeByService = false;
                        mAlreadyMuted = false;

                        try {
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

                            verifyAudioManager();

                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME), 0);
                            log(Log.INFO,TAG, "UNMUTE STREAM_MUSIC ");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        log(Log.INFO,TAG, "UNMUTED." + " mOldMediaVolume: " + SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME) + " OldringVolume: " + SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME));
                        try {
                            enableRingStream();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                new Thread(r).start();

/*
                try {
                    verifyAudioManager();
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }*/

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
                log(Log.INFO,TAG, "BroadCastFlags: mAlreadyMuted: " + mAlreadyMuted + " mInRingingSession: " + isRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION) + " mBugFixPatchForReceiverRegister: " + mBugFixPatchForReceiverRegister);

                if (mVolumeChangeByService)
                    mVolumeChangeByService = false;

                log(Log.INFO,TAG, "Exited BroadCast mOldMediaVolume: " + SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME) + " volumeDuringRun: " + volumeDuringRun);
            } else
                volumeChangeByMCButtons = false;
        }
    }
    //endregion


}
