package com.xtone.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;
import com.xtone.service.logic.CallIdleLogic;
import com.xtone.service.logic.CallIdleLogicImpl;
import com.xtone.service.logic.CallOffHookLogic;
import com.xtone.service.logic.CallOffHookLogicImpl;
import com.xtone.service.logic.CallRingingLogic;
import com.xtone.service.logic.CallRingingLogicImpl;
import com.xtone.utils.ContactsUtils;
import com.xtone.utils.UtilsFactory;


public class IncomingCallService extends Service {

    private static final String TAG = IncomingCallService.class.getSimpleName();

    private Logger log = LoggerFactory.getLogger();

    private CallIdleLogic callIdleLogic;

    private CallRingingLogic callRingingLogic;

    private CallOffHookLogic callOffHookLogic;

    public IncomingCallService() {
        callIdleLogic = new CallIdleLogicImpl();
        callRingingLogic = new CallRingingLogicImpl();
        callOffHookLogic = new CallOffHookLogicImpl();
    }

    @Override
    public void onCreate() {
        log.info(TAG, "onCreate()");
        prepareCallStateListener();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log.info(TAG, "OnStartcommand()");
        super.onStartCommand(intent, flags, startId);

        logIntentAction(intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void syncOnCallStateChange(int state, String incomingNumber) {
        log.info(TAG, String.format("Incoming phone number:[%s]", incomingNumber));

        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                callRingingLogic.handle(this, incomingNumber);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                callOffHookLogic.handle(this, incomingNumber);
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                callIdleLogic.handle(this, incomingNumber);
                break;
        }
    }

    private void prepareCallStateListener() {
        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            log.info(TAG, "Registering call state listener");
            telephonyManager.listen(new CallStateListener(), PhoneStateListener.LISTEN_CALL_STATE);
        } else {
            log.error(TAG, "Registered call state listener failed. TelephonyManager returned null");
        }
    }

    /**
     * Listener for call states
     * Listens for different call states
     */
    private class CallStateListener extends PhoneStateListener {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            syncOnCallStateChange(state, incomingNumber);
        }

    }

    private void logIntentAction(Intent intent) {
        String action = null;
        if (intent != null)
            action = intent.getAction();
        if (action != null)
            log.info(TAG, "Action:" + action);
    }

    public void setCallIdleLogic(CallIdleLogic callIdleLogic) {
        this.callIdleLogic = callIdleLogic;
    }

    public void setCallRingingLogic(CallRingingLogic callRingingLogic) {
        this.callRingingLogic = callRingingLogic;
    }

    public void setCallOffHookLogic(CallOffHookLogic callOffHookLogic) {
        this.callOffHookLogic = callOffHookLogic;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    //    private void actionThread(Intent intent) {
//        final Intent intentForThread = intent;
//        new Thread() {
//
//            @Override
//            public void run() {
//                if (intentForThread != null) {
//                    String action = intentForThread.getAction();
//                    switch (action) {
//
//                        case ACTION_STOP_RING: {
//                            stopSound();
//                        }
//                        break;
//                        case ACTION_START: {
//                        }
//                        break;
//                    }
//                }
//            }
//
//        }.start();
//    }

//    private void returnToPreviousRingerMode() {
//
//        try {
//            verifyAudioManager();
//            if (mAudioManager.getRingerMode() != getRingerMode()) {
//                log.info(TAG, "Set Ringer Mode back To Normal:" + getRingerMode() + " current RingerMode: " + mAudioManager.getRingerMode());
//                mAudioManager.setRingerMode(getRingerMode());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error(TAG, "Set Ringer Mode back To Normal error:" + e.getMessage());
//        }
//    }

//    private void enableRingStream() {
//
//        try {
//            verifyAudioManager();
//            if (getRingerMode() == AudioManager.RINGER_MODE_NORMAL)
//                mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0);
//            log.info(TAG, "mAudioManager.setStreamMute(AudioManager.STREAM_RING, false);");
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error(TAG, "Failed mAudioManager.setStreamMute(AudioManager.STREAM_RING, false); error:" + e.getMessage());
//        }
//        try {
//            if (getRingVolume() != mAudioManager.getStreamVolume(AudioManager.STREAM_RING)
//                    && getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {  // resuming previous Ring Volume
//                log.info(TAG, "AudioManager.STREAM_RING when ringermode is : " + String.valueOf(getRingerMode()));
//                mAudioManager.setStreamVolume(AudioManager.STREAM_RING, getRingVolume(), 0);
//                log.info(TAG, "mAudioManager.setStreamVolume(AudioManager.STREAM_RING, : " + String.valueOf(getRingVolume()));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error(TAG, "Failed  mAudioManager.setStreamVolume(AudioManager.STREAM_RING); error:" + e.getMessage());
//        }
//    }

//    private void disableRingStream() {
//        Context context = getApplicationContext();
//        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // check if the Device has Strict Ringing Capabilities that hard to be silent like in LG G4
////        boolean strictRingingCapabilitiesDevice = SettingsUtils.isStrictRingingCapabilitiesDevice(context);
////        if (strictRingingCapabilitiesDevice && mNotificationManager.isNotificationPolicyAccessGranted()) {
////            log.warn(TAG, "DND Allowed moving forward for silencing device. also String ringing enabled");
////            unlockMusicStreamDuringRinging();
////            correlateVibrateSettings();
////        }
//
//        try {
//            verifyAudioManager();
//            if (getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
//                mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0);
//                log.info(TAG, "mAudioManager.setStreamMute(AudioManager.STREAM_RING, true);");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error(TAG, "Failed mAudioManager.setStreamMute(AudioManager.STREAM_RING, true); error:" + e.getMessage());
//        }
//        try {
//            if (getRingerMode() == AudioManager.RINGER_MODE_NORMAL)
//                mAudioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
//            log.info(TAG, "mAudioManager.setStreamVolume(AudioManager.STREAM_RING, : 0");
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error(TAG, "Failed  mAudioManager.setStreamVolume(AudioManager.STREAM_RING); error:" + e.getMessage());
//        }
//    }

    //region Private classes and listeners

    /**
     * Receiver for volumes button presses
     * Responsible for muting the special call
     */
//    private class VolumeButtonReceiver extends BroadcastReceiver {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//            if (!volumeChangeByMCButtons) { // This is not a mute by hard button only volume change / mute by MC buttons , so ignore
//                int volumeDuringRun = (Integer) intent.getExtras().get("android.media.EXTRA_VOLUME_STREAM_VALUE");
//
//
//                if (mVolumeChangeByService) {
//                    mVolumeChangeByService = false;
//                }
//
//                log.info(TAG, "Exited BroadCast mOldMediaVolume: " + getRingVolume() + " volumeDuringRun: " + volumeDuringRun);
//            } else
//                volumeChangeByMCButtons = false;
//        }
//    }


}
