package com.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.utils.SharedPrefUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by Mor on 08/01/2016.
 */
public class OutgoingService extends AbstractStandOutService {

    private OutgoingCallReceiver mOutgoingCallReceiver;
    private static int TIME_TO_SLEEP_AVOIDING_BUGGY_STATE_IDLE = 1000;

    public OutgoingService() {
        super(OutgoingService.class.getSimpleName());
    }

    /* Service methods */

    @Override
    public void onCreate() {
        super.onCreate();

        if(mOutgoingCallReceiver == null) {

            mOutgoingCallReceiver = new OutgoingCallReceiver();
            IntentFilter filter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
            filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
            registerReceiver(mOutgoingCallReceiver, filter);
        }

        if(mVideoPreparedListener == null)
            mVideoPreparedListener = new OnVideoPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(true);
                    mp.setVolume(1.0f, 1.0f);
                    mp.start();
                }
            };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mOutgoingCallReceiver);
    }

    /**
     *
     * @param state The call state
     * @param incomingNumber The incoming number in case of an incoming call. Otherwise (outgoing call), null.
     */
    @Override
    protected void syncOnCallStateChange(int state, String incomingNumber) {

        switch(state)
        {
            case TelephonyManager.CALL_STATE_IDLE:
            case TelephonyManager.CALL_STATE_OFFHOOK:

                Log.i(TAG, "TelephonyManager IDLE=0, OFFHOOK=2. STATE WAS:"+state);
                if(mInRingingSession) {
                    Log.i(TAG, "TelephonyManager inside mInRingingSession IDLE=0, OFFHOOK=2. STATE WAS:"+state);

                    closeSpecialCallWindowWithoutRingtone();
                    mInRingingSession = false;
                }
                break;
        }
    }

    /* Private classes and listeners */

    /**
     * Listener for outgoing call state
     * Responsible for setting and starting special data on call and restoring previous data once call is terminated
     */
    private class OutgoingCallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
                String mOutgoingCallNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

                Log.i(TAG, "In ACTION_NEW_OUTGOING_CALL. mOutgoingCallNumber:"+ mOutgoingCallNumber);
                Log.i(TAG, "mInRingingSession="+mInRingingSession +  " mOutgoingCallNumber="+ mOutgoingCallNumber);
                if (!mInRingingSession && mOutgoingCallNumber !=null)
                {

                    try
                    {
                        String funTonePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.FUNTONE_FILEPATH, mOutgoingCallNumber);
                        final File funToneFile = new File(funTonePath);

                        if (funToneFile.exists())
                        {
                           startAudioSpecialCall(funTonePath);
                        }

                        String mediaFilePath
                                = SharedPrefUtils.getString(getApplicationContext(),
                                SharedPrefUtils.PROFILE_MEDIA_FILEPATH, mOutgoingCallNumber);

                        startMediaSpecialCall(mediaFilePath, mOutgoingCallNumber);

                        new Thread() {
                            @Override
                            public void run() {

                                try {
                                    Thread.sleep(TIME_TO_SLEEP_AVOIDING_BUGGY_STATE_IDLE);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                Log.i(TAG, "Setting mInRingingSession=true");
                                mInRingingSession = true;
                            }
                        }.start();
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        Log.e(TAG, "CALL_STATE_RINGING failed:"+e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    protected void playSound(Context context, Uri alert) {

        Log.i(TAG, "Playing funtone sound");
        mMediaPlayer = new MediaPlayer();
        try
        {
            mMediaPlayer.setDataSource(context, alert);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            mMediaPlayer.prepare();
            mMediaPlayer.setLooping(true);
            mMediaPlayer.start();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to play sound. Exception:" + e.getMessage());
        }
    }
}
