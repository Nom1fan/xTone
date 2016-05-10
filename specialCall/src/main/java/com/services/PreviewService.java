package com.services;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.utils.SharedPrefUtils;

import java.io.File;
import java.io.IOException;

import wei.mark.standout.ui.Window;

//import android.telephony.PreciseCallState;


/**
 * Created by Mor on 08/01/2016.
 */
public class PreviewService extends AbstractStandOutService {

    public static boolean isLive = false;

    public PreviewService() {
        super(PreviewService.class.getSimpleName());
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

        if (intent != null) {
            String action = intent.getAction();
            switch (action)
            {
                case ACTION_PREVIEW: {
                    Log.i(TAG, "ActionPreview Received");
                    mPreviewStart = true;
                    startPreviewWindow(intent);
                }
                break;
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public boolean onShow(int id, Window window) {
        super.onShow(id, window);  // at last so the volume will return to the previous(since when it was showed) , to make the volume always mute after Unhide move it to the Start of the method.

        Log.i(TAG, "mPreviewStart should mute : " + String.valueOf(mPreviewStart));
        setVolumeOnForPreview();
        return false;
    }

    @Override
    protected void syncOnCallStateChange(int state, String incomingNumber) {

    }
    //endregion

    //region AbstractStandOutService methods

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
        if (mPreviewAudioManager == null) {
            Log.i(TAG, "Audio manager was null , re-instantiated");
            mPreviewAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        }
        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME, mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        Log.i(TAG, "Preview MUSIC_VOLUME Original" + String.valueOf(mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));

        String visualMediaFilePath = intent.getStringExtra(AbstractStandOutService.PREVIEW_VISUAL_MEDIA);
        String audioMediafilePath = intent.getStringExtra(AbstractStandOutService.PREVIEW_AUDIO);
        String standoutWindowUserTitle = Constants.MY_ID(getApplicationContext());

        File audioMediaFile = new File(audioMediafilePath);
        startAudioMediaMC(audioMediafilePath);
        startVisualMediaMC(visualMediaFilePath, standoutWindowUserTitle, audioMediaFile.exists());
    }

    private void setVolumeOnForPreview() {

        Log.i(TAG, "setVolumeOnForPreview");

        try {
            mPreviewAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            mPreviewAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME), 0);
        } catch (Exception e) {
            Log.e(TAG, "setStreamVolume  STREAM_MUSIC failed. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
        }

        volumeChangeByMCButtons = true;
        isMuted = false;
        mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.unmute);//TODO : setImageResource need to be replaced ? memory issue ?
        mSpecialCallMutUnMuteBtn.bringToFront();

    }


    private void checkIntent(Intent intent) {
        String action = null;
        if (intent != null)
            action = intent.getAction();
        if (action != null)
            Log.i(TAG, "Action:" + action);
    }


    private void prepareVideoListener() {
        if (mVideoPreparedListener == null)
            mVideoPreparedListener = new OnVideoPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(true);
                    mp.setVolume(1.0f, 1.0f);
                    mp.start();
                    Log.i(TAG, "prepareVideoListener MUSIC_VOLUME Original" + String.valueOf(mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
                }
            };
    }

}
