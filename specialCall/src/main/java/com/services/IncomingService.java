package com.services;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.data.objects.Constants;
import com.data.objects.MediaCallData;
import com.enums.PermissionBlockListLevel;
import com.enums.SpecialMediaType;
import com.files.media.MediaFile;
import com.mediacallz.app.R;
import com.receivers.StartStandOutServicesFallBackReceiver;
import com.utils.AlarmUtils;
import com.utils.ContactsUtils;
import com.utils.MCBlockListUtils;
import com.utils.MCHistoryUtils;
import com.utils.MediaCallSessionUtils;
import com.utils.NotificationUtils;
import com.utils.Phone2MediaMapperUtils;
import com.utils.PhoneNumberUtils;
import com.utils.SettingsUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;
import com.utils.UtilityFactory;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import wei.mark.standout.StandOutWindow;

import static com.crashlytics.android.Crashlytics.log;
import static com.enums.PermissionBlockListLevel.CONTACTS_ONLY;
import static com.enums.PermissionBlockListLevel.NO_ONE;


public class IncomingService extends AbstractStandOutService {

    public static boolean isLive = false;
    private boolean mVolumeChangeByService = false;
    private boolean mAlreadyMuted = false;
    private boolean wasAnswered = false;
    public static final String ACTION_START_FOREGROUND = "com.services.IncomingService.ACTION_START_FOREGROUND";
    public static final String ACTION_STOP_FOREGROUND = "com.services.IncomingService.ACTION_STOP_FOREGROUND";
    private AlarmUtils alarmUtils = UtilityFactory.instance().getUtility(AlarmUtils.class);



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
        logIntentAction(intent);
        actionThread(intent);

        if (intent != null) {
            if (intent.getAction().equals(
                    ACTION_STOP_FOREGROUND)) {
                log(Log.INFO, TAG, "Received Stop Foreground Intent");
                stopForeground(true);
            } else if (intent.getAction().equals(
                    ACTION_START_FOREGROUND)) {
                log(Log.INFO, TAG, "Received Start Foreground Intent ");
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
    }

    @Override
    protected void prepareViewForSpecialCall(MediaFile.FileType fileType, String mediaFilePath) {
        super.prepareViewForSpecialCall(fileType, mediaFilePath);

        if (fileType == MediaFile.FileType.VIDEO) {

            try {
                disableRingStream();
                log(Log.INFO, TAG, "MUTE STREAM_RING ");
                log(Log.INFO, TAG, "VIDEO file detected MUTE Ring");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void playSound(Context context, Uri alert) {

        log(Log.INFO, TAG, "Playing ringtone sound");
        mediaPlayer = new MediaPlayer();
        try {
            try {
                mediaPlayer.setDataSource(context, alert);
            } catch (IOException e) {
                e.printStackTrace();
            }
            final AudioManager audioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != 0) {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.prepare();
                mediaPlayer.setLooping(true);
                mediaPlayer.start();

                //Crashlytics.log(Log.INFO,TAG, " Ringtone registerVolumeReceiver");
                //registerVolumeReceiver();

            }
        } catch (IOException e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Failed to play sound. Exception:" + e.getMessage());
        }
    }

    @Override
    protected synchronized void syncOnCallStateChange(int state, String incomingNumber) {

        Context context = getApplicationContext();
        incomingNumber = PhoneNumberUtils.toValidLocalPhoneNumber(incomingNumber);
        boolean inRingingSession = MediaCallSessionUtils.isIncomingRingingInSession(context);
        log(Log.INFO, TAG, "Inside syncOnCallStateChange, incoming phone number : " + incomingNumber);
        mIncomingOutgoingNumber = incomingNumber;
        // Checking if number is in black list
        log(Log.INFO, TAG, " isInRingingSession:" + inRingingSession);
        boolean isBlocked = MCBlockListUtils.IsMCBlocked(incomingNumber, context);

        handleBlockedNumber(state, incomingNumber, isBlocked);

        if (!isBlocked || inRingingSession) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    handleCallStateRinging(incomingNumber);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    handleCallStateOffHook();
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    handleCallStateIdle();
                    break;
            }
        }
    }

    private void handleCallStateIdle() {
        log(Log.INFO, TAG, "TelephonyManager.CALL_STATE_IDLE");
        Context context = getApplicationContext();

        boolean isIncomingRingingInSession = MediaCallSessionUtils.isIncomingRingingInSession(context);

        if (wasAnswered || isIncomingRingingInSession) {
            log(Log.INFO, TAG, "INSIDE MC TelephonyManager.CALL_STATE_IDLE");
            stopVibrator();
            closeSpecialCallWindowAndRingtone();

            Runnable r = new Runnable() {
                public void run() {
                    log(Log.INFO, TAG, "making wasAnswered = false");
                    try {
                        Thread.sleep(2000, 0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    enableRingStream();
                    wasAnswered = false;
                }
            };
            new Thread(r).start();
        }
    }

    private void handleCallStateOffHook() {
        log(Log.INFO, TAG, "TelephonyManager.CALL_STATE_OFFHOOK");
        Context context = getApplicationContext();

        boolean isIncomingRingingInSession = MediaCallSessionUtils.isIncomingRingingInSession(context);
        if (isIncomingRingingInSession) {
            log(Log.INFO, TAG, "INSIDE MC TelephonyManager.CALL_STATE_OFFHOOK");
            log(Log.INFO, TAG, "making wasAnswered = true");
            wasAnswered = true;
            stopVibrator();
            closeSpecialCallWindowAndRingtone();
        }
    }

    private void handleCallStateRinging(String incomingNumber) {
        log(Log.INFO, TAG, "TelephonyManager.CALL_STATE_RINGING: " + incomingNumber);
        Context context = getApplicationContext();

        if (shouldStartMediaCall(incomingNumber)) {
            log(Log.INFO, TAG, "INSIDE MC TelephonyManager.CALL_STATE_RINGING: " + incomingNumber);
            try {
                UI_Utils.dismissAllStandOutWindows(context);
                super.setIncomingWindowDisplayed(true);
                MediaCallSessionUtils.setIncomingRingingSession(context, true);
                MediaCallData mediaCallData = prepareMediaCallData(incomingNumber, context);

                mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                backupRingSettings(mediaCallData.shouldMuteRing());
                backupMusicVolume();

                runMediaCall(mediaCallData);
            } catch (Exception e) {
                e.printStackTrace();
                log(Log.ERROR, TAG, "CALL_STATE_RINGING failed:" + e.getMessage());
            }
        }
    }

    private boolean shouldStartMediaCall(String incomingNumber) {
        Context context = getApplicationContext();
        boolean isIncomingRingingInSession = MediaCallSessionUtils.isIncomingRingingInSession(context);
        boolean isOutgoingRingingInSession = MediaCallSessionUtils.isOutgoingRingingInSession(context);
        boolean isValidPhoneNumber = PhoneNumberUtils.isValidPhoneNumber(incomingNumber);
        return !isIncomingRingingInSession && !isOutgoingRingingInSession && isValidPhoneNumber && (!wasAnswered);
    }

    @NonNull
    private MediaCallData prepareMediaCallData(String incomingNumber, Context context) {
        String mediaFilePath = Phone2MediaMapperUtils.getCallerVisualMedia(context, incomingNumber);
        String ringtonePath = Phone2MediaMapperUtils.getCallerAudioMedia(context, incomingNumber);

        boolean ringtoneExists = new File(ringtonePath).exists() && !mediaFileUtils.isAudioFileCorrupted(ringtonePath, context);
        boolean visualMediaExists = new File(mediaFilePath).exists() && !mediaFileUtils.isVideoFileCorrupted(mediaFilePath, context);
        boolean shouldMuteRing = shouldMuteRing(mediaFilePath, ringtoneExists, visualMediaExists);

        MediaCallData mediaCallData = new MediaCallData();
        mediaCallData.setVisualMediaFilePath(mediaFilePath);
        mediaCallData.setAudioMediaFilePath(ringtonePath);
        mediaCallData.setDoesAudioMediaExist(ringtoneExists);
        mediaCallData.setDoesVisualMediaExist(visualMediaExists);
        mediaCallData.setShouldMuteRing(shouldMuteRing);
        mediaCallData.setPhoneNumber(incomingNumber);
        return mediaCallData;
    }

    private void runMediaCall(MediaCallData mediaCallData) {
        Context context = getApplicationContext();
        if (mediaCallData.hasMedia()) {
            log(Log.INFO, TAG, "MEDIA EXIST for incoming number:" + mediaCallData);
            if ((SettingsUtils.getAskBeforeShowingMedia(context) && !isAskBeforeMediaShowStandOut()) ||
                    !isHideResizeWindowForStandOut()) {
                runAskBeforeShowMedia(mediaCallData);
            } else {
                runIncomingMCMedia(mediaCallData);
            }
        } else {
            MediaCallSessionUtils.setIncomingRingingSession(context, false);
            setIncomingWindowDisplayed(false);
            log(Log.INFO, TAG, "NO MEDIA ! for incoming number:" + mediaCallData);
        }
    }

    private boolean shouldMuteRing(String mediaFilePath, boolean ringtoneExists, boolean visualMediaExists) {
        boolean willMuteRing = false;
        if (visualMediaExists) {
            try {
                MediaFile managedFile = new MediaFile(new File(mediaFilePath));
                MediaFile.FileType fType = managedFile.getFileType();
                if (fType.equals(MediaFile.FileType.VIDEO)) {
                    willMuteRing = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                log(Log.INFO, TAG, "Video Is missing or corrupted. when checking visualMediaExists File Path: " + mediaFilePath);
            }
        }

        willMuteRing = ringtoneExists || willMuteRing;
        return willMuteRing;
    }

    private void handleBlockedNumber(int state, String incomingNumber, boolean isBlocked) {
        Context context = getApplicationContext();
        if (isBlocked) {

            if (state == TelephonyManager.CALL_STATE_RINGING) {
                UI_Utils.dismissAllStandOutWindows(context);
                contactName = ContactsUtils.getContactName(context, incomingNumber);

                PermissionBlockListLevel permissionLevel = SettingsUtils.getWhoCanMCMe(context);

                // Specific number or contact was blocked
                if (!permissionLevel.equals(CONTACTS_ONLY) && !permissionLevel.equals(NO_ONE)) {
                    if (contactName.isEmpty()) {
                        UI_Utils.callToast("MediaCallz: " + incomingNumber + " Media Blocked", Color.RED, Toast.LENGTH_SHORT, context);
                    } else {
                        UI_Utils.callToast("MediaCallz: " + contactName + " Media Blocked", Color.RED, Toast.LENGTH_SHORT, context);
                    }
                }
            }

        }
    }

    private void runAskBeforeShowMedia(MediaCallData mediaCallData) {
        String incomingNumber = mediaCallData.getPhoneNumber();
        SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.ASK_BEFORE_MEDIA_SHOW_FOR_STANDOUT, true);
        setDisableVolButtons(getApplicationContext(), true);
        log(Log.INFO, TAG, "Run Ask Before Show Media");
        contactName = ContactsUtils.getContactName(getApplicationContext(), incomingNumber);
        mContactTitleOnWindow = (contactName != null && !contactName.equals("") ? contactName + " " + incomingNumber : incomingNumber);
        Random r = new Random();
        int randomWindowId = r.nextInt(Integer.MAX_VALUE);  // fixing a bug: when the same ID the window isn't released good enough so we need to make a different window in the mean time
        prepareAskBeforeShowViewForSpecialCall(mediaCallData);
        Intent i = new Intent(this, this.getClass());
        i.putExtra("id", randomWindowId);
        i.setAction(StandOutWindow.ACTION_SHOW);
        startService(i);
    }

    private void prepareAskBeforeShowViewForSpecialCall(final MediaCallData mediaCallData) {
        log(Log.INFO, TAG, "Preparing SpecialCall view");

        // Attempting to induce garbage collection
        System.gc();

        prepareRelativeLayout();

        mSpecialCallView = new ImageView(this);
        mSpecialCallView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        //     ((ImageView)mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_XY); STRECTH IMAGE ON FULL SCREEN <<< NOT SURE IT's GOOD !!!!!
        ((ImageView) mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_CENTER); // <<  just place the image Center of Window and fit it with ratio

        if (!isHideResizeWindowForStandOut()) {
            ((ImageView) mSpecialCallView).setImageResource(R.drawable.first_show_window);

        } else
            ((ImageView) mSpecialCallView).setImageResource(R.drawable.profile_media_anim);

        mSpecialCallView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.HIDE_RESIZE_WINDOW_FOR_STANDOUT, true);
                log(Log.INFO, TAG, "Asked to show media ! and he Said YES !! ");
                // close(randomWindowId);
                closeAll();
                runIncomingMCMedia(mediaCallData);
            }
        });


        mRelativeLayout.addView(mSpecialCallView);
    }

    private void runIncomingMCMedia(MediaCallData mediaCallData) {
        Context context = getApplicationContext();
        log(Log.INFO, TAG, "runIncomingMCMedia:" + mediaCallData);
        setAskBeforeMediaShowStandout(false);
        setIncomingWindowDisplayed(true);
        MediaCallSessionUtils.setIncomingRingingSession(context, true);

        verifyAudioManager();

        showFirstTime = true;

        final File ringtoneFile = new File(mediaCallData.getAudioMediaFilePath());

        try {
            setupStandOutWindowMusicVolumeLogic();

        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Failed to set stream volume:" + e.getMessage());
        }

        //Check if Mute Was Needed if not return to UnMute.
        if (mediaCallData.doesAudioMediaExist()) {

            disableRingStream();
            setDisableVolButtons(context, false);
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
            setDisableVolButtons(context, true);
            Log.i(TAG, "No Ringtone !!");
        }

        setTempMd5ForCallRecord(mediaCallData);

        startVisualMediaMC(mediaCallData);

        MCHistoryUtils.reportMC(
                context,
                mediaCallData.getPhoneNumber(),
                Constants.MY_ID(context),
                mediaCallData.doesVisualFileExist() ? mediaCallData.getVisualMediaFilePath() : null,
                mediaCallData.doesAudioMediaExist() ? mediaCallData.getAudioMediaFilePath() : null,
                SpecialMediaType.CALLER_MEDIA);
    }

    private void setupStandOutWindowMusicVolumeLogic() {

        int ringVolume = getRingVolume();

        // TODO maybe add GetRingerMode rule just in case volume is above 0 and mode is silent... never saw that but maybe we should cover this scenario if there is some weird devices :/ for now no need... need to check
        // Setting music volume to equal the ringtone volume
        mVolumeChangeByService = true;
        // Get the current ringer volume as a percentage of the max ringer volume.
        int maxRingerVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        Log.d(TAG, "maxRingerVolume: " + maxRingerVolume);
        double proportion = ringVolume / (double) maxRingerVolume;

        // Calculate a desired music volume as that same percentage of the max music volume.
        int maxMusicVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "maxMusicVolume: " + maxMusicVolume);
        int desiredMusicVolume = (int) (proportion * maxMusicVolume);

        // Set the music stream volume.
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, desiredMusicVolume, 0 /*flags*/);

        Log.d(TAG, "STREAM_MUSIC Change : " + (desiredMusicVolume));

    }
    //endregion

    //region Internal helper methods
    private void isForegroundAndAlarmNeeded() {
        boolean shouldStartForeground = false;//true;//TODO remove it when we know how to keep it working against SPCM or other memory services//SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.GENERAL,SharedPrefUtils.STRICT_MEMORY_MANAGER_DEVICES);
        log(Log.INFO, TAG, "shouldStartForeground:" + shouldStartForeground);
        if (shouldStartForeground) {
            //TODO ForeGroundService needed & Alarm Needed or this is solved after memory leakage solved????

            startForegroundService();

            alarmUtils.setAlarm(this, StartStandOutServicesFallBackReceiver.class, 30);
        }
    }

    public void startForegroundService() {
        startForeground(NotificationUtils.FOREGROUND_NOTIFICATION_ID, NotificationUtils.getCompatNotification(getApplicationContext()));
        log(Log.INFO, TAG, "STARTED FOREGROUND");
    }

    private void checkIfItsFallBackReceiverIntent(Intent intent) {
        if (intent != null) {

            //release wake lock from fallbackreceiver
            if (intent.getBooleanExtra(StartStandOutServicesFallBackReceiver.WAKEFUL_INTENT, true))
                StartStandOutServicesFallBackReceiver.completeWakefulIntent(intent);

            String incomingPhoneNumber = intent.getStringExtra(StartStandOutServicesFallBackReceiver.INCOMING_PHONE_NUMBER_KEY);
            log(Log.INFO, TAG, "FallBackReceiver Gives incoming number:" + incomingPhoneNumber);

            // do you start from FallBackReceiver ??
            if (incomingPhoneNumber != null && !incomingPhoneNumber.isEmpty()) {
                log(Log.INFO, TAG, "sending fallbackReceiver incoming number to SyncOnCallState: " + incomingPhoneNumber);
                //Initiating syncOnCallStateChange
                syncOnCallStateChange(TelephonyManager.CALL_STATE_RINGING, incomingPhoneNumber);
            }
        }

    }

    private void logIntentAction(Intent intent) {
        String action = null;
        if (intent != null)
            action = intent.getAction();
        if (action != null)
            log(Log.INFO, TAG, "Action:" + action);
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
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer = mp;
                        mediaPlayer.setLooping(true);
                        mediaPlayer.setVolume(1.0f, 1.0f);
                        mediaPlayer.start();
                        log(Log.INFO, TAG, " Video registerVolumeReceiver");
                        registerVolumeReceiver();
                    }
                };

            // if(mVolumeButtonReceiver == null)
            //     mVolumeButtonReceiver = new VolumeButtonReceiver();

        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, e.getMessage());
        }
    }

    private void returnToPreviousRingerMode() {

        try {
            verifyAudioManager();
            if (mAudioManager.getRingerMode() != getRingerMode()) {
                log(Log.INFO, TAG, "Set Ringer Mode back To Normal:" + getRingerMode() + " current RingerMode: " + mAudioManager.getRingerMode());
                mAudioManager.setRingerMode(getRingerMode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Set Ringer Mode back To Normal error:" + e.getMessage());
        }


    }

    private void enableRingStream() {

        try {
            verifyAudioManager();
            if (getRingerMode() == AudioManager.RINGER_MODE_NORMAL)
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0);
            log(Log.INFO, TAG, "mAudioManager.setStreamMute(AudioManager.STREAM_RING, false);");
        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Failed mAudioManager.setStreamMute(AudioManager.STREAM_RING, false); error:" + e.getMessage());
        }
        try {
            if (getRingVolume() != mAudioManager.getStreamVolume(AudioManager.STREAM_RING)
                    && getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {  // resuming previous Ring Volume
                log(Log.INFO, TAG, "AudioManager.STREAM_RING when ringermode is : " + String.valueOf(getRingerMode()));
                mAudioManager.setStreamVolume(AudioManager.STREAM_RING, getRingVolume(), 0);
                log(Log.INFO, TAG, "mAudioManager.setStreamVolume(AudioManager.STREAM_RING, : " + String.valueOf(getRingVolume()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Failed  mAudioManager.setStreamVolume(AudioManager.STREAM_RING); error:" + e.getMessage());
        }
    }

    private void disableRingStream() {
        Context context = getApplicationContext();
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // check if the Device has Strict Ringing Capabilities that hard to be silent like in LG G4
        boolean strictRingingCapabilitiesDevice = SettingsUtils.isStrictRingingCapabilitiesDevice(context);
        if (strictRingingCapabilitiesDevice && mNotificationManager.isNotificationPolicyAccessGranted()) {
            log(Log.WARN, TAG, "DND Allowed moving forward for silencing device. also String ringing enabled");
            unlockMusicStreamDuringRinging();
            correlateVibrateSettings();
        }

        try {
            verifyAudioManager();
            if (getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0);
                log(Log.INFO, TAG, "mAudioManager.setStreamMute(AudioManager.STREAM_RING, true);");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Failed mAudioManager.setStreamMute(AudioManager.STREAM_RING, true); error:" + e.getMessage());
        }
        try {
            if (getRingerMode() == AudioManager.RINGER_MODE_NORMAL)
                mAudioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
            log(Log.INFO, TAG, "mAudioManager.setStreamVolume(AudioManager.STREAM_RING, : 0");
        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Failed  mAudioManager.setStreamVolume(AudioManager.STREAM_RING); error:" + e.getMessage());
        }
    }

    /**
     * in certain devices , like LG G4 G3 ... the music stream is locked during ringing. this code unlocks it.
     * in other devices it doesn't hurt anything
     */
    private void unlockMusicStreamDuringRinging() {

        verifyAudioManager();
        log(Log.INFO, TAG, "unlockMusicStreamDuringRinging , getRingerMode: " + mAudioManager.getRingerMode());
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log(Log.INFO, TAG, "unlockMusicStreamDuringRinging , Setting To Silent");

        try {
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);  // SOLUTION For LG G4 that needs another motivation to be silent (if removed the audio isn't heared in LG G4 you need to press the volume hard keys to silent manually , this fixes it)
        } catch (Exception e) {
            e.printStackTrace();
        }

        log(Log.INFO, TAG, "unlockMusicStreamDuringRinging , getRingerMode: " + mAudioManager.getRingerMode());
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log(Log.INFO, TAG, "unlockMusicStreamDuringRinging , Setting Back To Normal");

        try {
            mAudioManager.setRingerMode(getRingerMode());  // SOLUTION For LG G4 that needs another motivation to be silent (if removed the audio isn't heared in LG G4 you need to press the volume hard keys to silent manually , this fixes it)
        } catch (Exception e) {
            e.printStackTrace();
        }

        log(Log.INFO, TAG, "unlockMusicStreamDuringRinging , getRingerMode: " + mAudioManager.getRingerMode());
    }

    private void correlateVibrateSettings() {

        //get vibrate settings and vibrate if needed.
        boolean isVibrateOn = Settings.System.getInt(this.getApplicationContext().getContentResolver(), "vibrate_when_ringing", 0) != 0;
        log(Log.INFO, TAG, "isVibrateOn: " + isVibrateOn);
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
        } catch (Exception e) {
            e.printStackTrace();
        }


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
        final Context context = getApplicationContext();
        if (MediaCallSessionUtils.isIncomingRingingInSession(context)) {

            try {

                enableRingStream();

                try {
                    log(Log.INFO, TAG, "mediaPlayer.stop(); closeSpecialCallWindowAndRingtone");
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.setVolume(0, 0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    returnToPreviousRingerMode();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                MediaCallSessionUtils.setIncomingRingingSession(context, false);

                Runnable r = new Runnable() {
                    public void run() {

                        mVolumeChangeByService = false;
                        mAlreadyMuted = false;

                        try {
                            if (mediaPlayer != null)
                                mediaPlayer.stop();
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

                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, getMusicVolume(), 0);
                            log(Log.INFO, TAG, "UNMUTE STREAM_MUSIC ");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        log(Log.INFO, TAG, "UNMUTED." + " mOldMediaVolume: " + getMusicVolume() + " OldringVolume: " + getRingVolume());
                        try {
                            enableRingStream();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                new Thread(r).start();

                setAskBeforeMediaShowStandout(false);
                Intent i = new Intent(context, IncomingService.class);
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

                log(Log.INFO, TAG, "BroadCastFlags: mAlreadyMuted: " + mAlreadyMuted + " mInRingingSession: " + MediaCallSessionUtils.isIncomingRingingInSession(context));

                if (mVolumeChangeByService) {
                    mVolumeChangeByService = false;
                }

                log(Log.INFO, TAG, "Exited BroadCast mOldMediaVolume: " + getRingVolume() + " volumeDuringRun: " + volumeDuringRun);
            } else
                volumeChangeByMCButtons = false;
        }
    }
    //endregion


}
