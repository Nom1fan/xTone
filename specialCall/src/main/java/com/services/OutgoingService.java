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
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.special.app.R;
import com.utils.SharedPrefUtils;

import java.io.File;
import java.io.IOException;

import FilesManager.FileManager;
import wei.mark.standout.StandOutWindow;

/**
 * Created by Mor on 08/01/2016.
 */
public class OutgoingService extends AbstractStandOutService {

    private OutgoingCallReceiver mOutgoingCallReceiver;
    private static int TIME_TO_SLEEP_AVOIDING_BUGGY_STATE_IDLE = 1000;
    private  boolean isMuted=false;
    protected ImageView mSpecialCallMuteBtn;
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
                    Log.i(TAG, "TelephonyManager inside mInRingingSession IDLE=0, OFFHOOK=2. STATE WAS:" + state);
                    closeSpecialCallWindowWithoutRingtone();
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    mInRingingSession=false;
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
                        mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                        String funTonePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.FUNTONE_FILEPATH, mOutgoingCallNumber);
                        final File funToneFile = new File(funTonePath);

                        String mediaFilePath
                                = SharedPrefUtils.getString(getApplicationContext(),
                                SharedPrefUtils.PROFILE_MEDIA_FILEPATH, mOutgoingCallNumber);

                        startMediaSpecialCall(mediaFilePath, mOutgoingCallNumber);

                        if (funToneFile.exists())
                        {
                            startAudioSpecialCall(funTonePath);
                        }

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
    protected void prepareViewForSpecialCall(FileManager.FileType fileType , String mediaFilePath, String callNumber) {
        super.prepareViewForSpecialCall(fileType, mediaFilePath, callNumber);

        prepareMuteBtn();

        mRelativeLayout.addView(mSpecialCallMuteBtn);

        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            Log.i(TAG, " android.os.Build.VERSION.SDK_INT : " + String.valueOf(android.os.Build.VERSION.SDK_INT) + " Build.VERSION_CODES.KITKAT = " + Build.VERSION_CODES.KITKAT );
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            isMuted=true;
            mSpecialCallMuteBtn.setImageResource(R.drawable.mute);
            mSpecialCallMuteBtn.bringToFront();
        }

       /* Intent i = new Intent(this, this.getClass());
        //i.putExtra("id", mID);
        i.setAction(StandOutWindow.ACTION_SHOW);
        startService(i);*/
    }

    private void prepareMuteBtn()
    {
        Log.i(TAG, "Preparing Mute Button");

        //ImageView for Closing Special Incoming Call
        mSpecialCallMuteBtn = new ImageView(this);
        RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp1.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp1.addRule(RelativeLayout.ALIGN_BOTTOM);
        lp1.addRule(RelativeLayout.ALIGN_RIGHT);
        lp1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        mSpecialCallMuteBtn.setImageResource(R.drawable.unmute);
        mSpecialCallMuteBtn.setBackgroundColor(Color.WHITE);
        mSpecialCallMuteBtn.setLayoutParams(lp1);
        mSpecialCallMuteBtn.setClickable(true);
        mSpecialCallMuteBtn.bringToFront();
        mSpecialCallMuteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isMuted) {
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    isMuted = false;
                    mSpecialCallMuteBtn.setImageResource(R.drawable.unmute);
                    mSpecialCallMuteBtn.bringToFront();
                    Log.i(TAG, "UNMUTE by button");
                } else {
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                    isMuted = true;
                    mSpecialCallMuteBtn.setImageResource(R.drawable.mute);
                    mSpecialCallMuteBtn.bringToFront();
                    Log.i(TAG, "MUTE by button");
                }
            }
        });
    }

    @Override
    protected void playSound(Context context, Uri alert) {

        Log.i(TAG, "Playing funtone sound");
        mMediaPlayer = new MediaPlayer();
        try
        {
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
}
