package com_international.services;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com_international.data.objects.Constants;
import com_international.data.objects.MediaCallData;
import com_international.mediacallz.app.R;

import java.io.File;
import java.io.IOException;

import wei.mark.standout.ui.Window;

import static com.crashlytics.android.Crashlytics.log;

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
            switch (action) {
                case ACTION_PREVIEW: {
                    log(Log.INFO, TAG, "ActionPreview Received");
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

        log(Log.INFO, TAG, "mPreviewStart should mute : " + String.valueOf(mPreviewStart));
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

        log(Log.INFO, TAG, "Playing funtone sound");
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(context, alert);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);
            mediaPlayer.start();


        } catch (IOException e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Failed to play sound. Exception:" + e.getMessage());
        }
    }
    //endregion

    //region Internal helper methods
    protected void startPreviewWindow(Intent intent) {
        log(Log.INFO, TAG, "startPreviewWindow");
        Context context = getApplicationContext();


        if (mPreviewAudioManager == null) {
            log(Log.INFO, TAG, "Audio manager was null , re-instantiated");
            mPreviewAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        int musicVolume = mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        setMusicVolume(musicVolume);
        log(Log.INFO, TAG, "Preview MUSIC_VOLUME Original" + musicVolume);

        String visualMediaFilePath = intent.getStringExtra(AbstractStandOutService.PREVIEW_VISUAL_MEDIA);
        String audioMediaFilePath = intent.getStringExtra(AbstractStandOutService.PREVIEW_AUDIO);
        String myPhoneNumber = Constants.MY_ID(context);

        boolean funToneExists = new File(audioMediaFilePath).exists() && !mediaFileUtils.isAudioFileCorrupted(audioMediaFilePath, context);
        boolean visualMediaExists = new File(visualMediaFilePath).exists() && !mediaFileUtils.isVideoFileCorrupted(visualMediaFilePath, context);

        MediaCallData mediaCallData = new MediaCallData();
        log(Log.INFO, TAG, "startPreviewWindow:" + mediaCallData);
        startAudioMediaMC(audioMediaFilePath);
        mediaCallData.setVisualMediaFilePath(visualMediaFilePath);
        mediaCallData.setAudioMediaFilePath(audioMediaFilePath);
        mediaCallData.setDoesVisualMediaExist(visualMediaExists);
        mediaCallData.setDoesAudioMediaExist(funToneExists);
        mediaCallData.setPhoneNumber(myPhoneNumber);
        startVisualMediaMC(mediaCallData);
    }

    private void setVolumeOnForPreview() {

        log(Log.INFO, TAG, "setVolumeOnForPreview");

        try {
            mPreviewAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            mPreviewAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, getMusicVolume(), 0);
        } catch (Exception e) {
            log(Log.ERROR, TAG, "setStreamVolume  STREAM_MUSIC failed. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
        }

        volumeChangeByMCButtons = true;
        isMuted = false;
        mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.unmute_speaker_anim);//TODO : setImageResource need to be replaced ? memory issue ?
        mSpecialCallMutUnMuteBtn.bringToFront();

    }

    private void checkIntent(Intent intent) {
        String action = null;
        if (intent != null)
            action = intent.getAction();
        if (action != null)
            log(Log.INFO, TAG, "Action:" + action);
    }


    private void prepareVideoListener() {
        if (mVideoPreparedListener == null)
            mVideoPreparedListener = new OnVideoPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    int streamVolume = mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    mp.setLooping(true);
                    mp.setVolume(1.0f, 1.0f);
                    mp.start();
                    log(Log.INFO,TAG, "prepareVideoListener MUSIC_VOLUME Original" + streamVolume);
                }
            };
    }

}
