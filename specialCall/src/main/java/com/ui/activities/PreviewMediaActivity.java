package com.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.mediacallz.app.R;
import com.semantive.waveformandroid.waveform.WaveformFragment;
import com.services.AbstractStandOutService;
import com.services.PreviewService;
import com.utils.BitmapUtils;
import com.utils.MediaFilesUtils;
import com.utils.SharedPrefUtils;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.util.Timer;
import java.util.TimerTask;

import com.files.media.MediaFile;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by rony on 29/01/2016.
 */
public class PreviewMediaActivity extends AppCompatActivity {

    public static final String MANAGED_MEDIA_FILE = "MANAGED_MEDIA_FILE";
    public static final String RESULT_FILE = "RESULT_FILE";
    private final static int POOLING_INTERVAL_MS = 100;

    private static final String TAG = PreviewMediaActivity.class.getSimpleName();
    private MediaFile _managedFile;
    private float _oldPosition =0;
    private int _moveLength = 0;
    private static boolean isPreviewDisplaying = false;
    private ImageButton previewThumbnail;
    private static ImageButton _previewVideoTrimFile;
    private ImageButton _imageButton;
    private MediaFile.FileType fType;
    private final int MIN_MILISECS_FOR_AUDIO_EDIT = 3000;
    protected static int startInMili;
    protected static int endInMili;
    private Timer timer;
    private boolean isActive;
    private VideoView trimVideoView;

    //region Activity methods (onCreate(), onPause()...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log(Log.INFO,TAG, "onCreate()");
        Intent intent = getIntent();
        _managedFile = (MediaFile) intent.getSerializableExtra(MANAGED_MEDIA_FILE);
        initializePreviewMediaUI();

    }

    private void initializePreviewMediaUI() {

        closePreview();

        setContentView(R.layout.preview_before_upload);

        TextView fileType = (TextView) findViewById(R.id.upload_file_type);
        TextView fileName = (TextView) findViewById(R.id.upload_file_name);
        fileName.setText(MediaFilesUtils.getFileNameWithExtension(_managedFile.getFileFullPath()));
        fType = _managedFile.getFileType();

        prepareUploadBtn();

        prepareCancelBtn();

        preparePlayPreviewBtn();

        switch (fType) {
            case AUDIO:
                handlePreviewAudio(fileType);
                break;

            case VIDEO:
                handlePreviewVideo(fileType);
                break;

            case IMAGE:
                handlePreviewImage(fileType);
                break;
        }


    }

    private void preparePlayPreviewBtn() {
        previewThumbnail = (ImageButton) findViewById(R.id.playPreview);
        previewThumbnail.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (isPreviewDisplaying) {
                    closePreview();
                    isPreviewDisplaying = false;
                    previewThumbnail.setImageResource(R.drawable.play_preview_anim);
                } else {
                    startPreviewStandoutWindow(_managedFile.getFileFullPath() , fType);
                    isPreviewDisplaying = true;
                    previewThumbnail.setImageResource(R.drawable.stop_preview_anim);
                }
            }
        });
    }

    private void prepareCancelBtn() {
        ImageButton cancel = (ImageButton) findViewById(R.id.cancel_btn);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closePreview();
                getSupportFragmentManager().popBackStack(); // closes the audio editor fragment in case it is still playing audio
                finish();
            }
        });
    }

    private void prepareUploadBtn() {
        ImageButton upload = (ImageButton) findViewById(R.id.upload_btn);
        upload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closePreview();

                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_VIDEO_START_TRIM_IN_MILISEC, startInMili);
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_VIDEO_END_TRIM_IN_MILISEC, endInMili);
                startInMili=0;
                endInMili=0;

                getSupportFragmentManager().popBackStack(); // closes the audio editor fragment in case it is still playing audio

                returnFile(_managedFile);
            }
        });
    }

    private void handlePreviewImage(TextView fileType) {
        fileType.setText(getResources().getString(R.string.fileType_image));

        if (!_managedFile.getFileExtension().toLowerCase().contains("gif")) {
            ImageButton rotate = (ImageButton) findViewById(R.id.rotate_button);
            SharedPrefUtils.setInt(getApplicationContext(),SharedPrefUtils.GENERAL,SharedPrefUtils.IMAGE_ROTATION_DEGREE,0);
            rotate.setClickable(true);
            rotate.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    _imageButton.setRotation((_imageButton.getRotation() + 90)%360);
                    SharedPrefUtils.setInt(getApplicationContext(),SharedPrefUtils.GENERAL,SharedPrefUtils.IMAGE_ROTATION_DEGREE,Math.round(_imageButton.getRotation()));
                }
            });
            rotate.setVisibility(View.VISIBLE);
            previewThumbnail.setVisibility(View.INVISIBLE);
            previewThumbnail.setClickable(false);
            TextView rotateTextview = (TextView) findViewById(R.id.rotate_textview);
            rotateTextview.setVisibility(View.VISIBLE);
        }
    }

    private void handlePreviewVideo(TextView fileType) {
        fileType.setText(getResources().getString(R.string.fileType_video));

        final ImageButton edit_video = (ImageButton) findViewById(R.id.editAudio);
        edit_video.setClickable(true);
        edit_video.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_START_TRIM_IN_MILISEC, 0);
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_END_TRIM_IN_MILISEC, 0);

                // previewThumbnail.setVisibility(View.INVISIBLE);
                previewThumbnail.setClickable(false);
                _previewVideoTrimFile = (ImageButton) findViewById(R.id.playVideoTrimPreview);
                _previewVideoTrimFile.setVisibility(View.VISIBLE);

                trimVideoView = (VideoView) findViewById(R.id.trimvideo_view);

                trimVideoView.setVideoURI(Uri.parse(_managedFile.getFileFullPath()));
                trimVideoView.requestFocus();
                trimVideoView.setVisibility(View.VISIBLE);
                trimVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        trimVideoView.setBackgroundColor(Color.TRANSPARENT);
                    }
                });

                RangeSeekBar videoSeekBar = (RangeSeekBar) findViewById(R.id.seekbar);
                videoSeekBar.setVisibility(View.VISIBLE);

                long durationInMili = MediaFilesUtils.getFileDurationInMilliSeconds(getApplicationContext(), _managedFile);
                endInMili =  Integer.parseInt(String.valueOf(durationInMili));
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_END_TRIM_IN_MILISEC,endInMili);

                videoSeekBar.setRangeValues(0, durationInMili);
                videoSeekBar.setNotifyWhileDragging(true);

                videoSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
                    @Override
                    public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Number minValue, Number maxValue) {
                        startInMili= (int)minValue;
                        endInMili= (int)maxValue;
                        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_START_TRIM_IN_MILISEC, (int)minValue);
                        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_END_TRIM_IN_MILISEC,(int) maxValue);

                        Log.i(TAG,"setOnRangeSeekBarChangeListener " + minValue + " " +maxValue  );

                        int current_pos = (int)minValue; // in mili
                        trimVideoView.seekTo(current_pos);
                    }
                } );

                _previewVideoTrimFile.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {

                        if (isPreviewDisplaying) {
                            trimVideoView.pause();
                            isActive = false;
                            cancelProgressPooling();

                            isPreviewDisplaying = false;
                            _previewVideoTrimFile.setImageResource(R.drawable.play_preview_anim);
                        } else {
                            trimVideoView.seekTo(SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_START_TRIM_IN_MILISEC));
                            trimVideoView.start();
                            initVideoProgressPooling(SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_END_TRIM_IN_MILISEC));
                            isActive = true;
                            isPreviewDisplaying = true;
                            _previewVideoTrimFile.setImageResource(R.drawable.stop_preview_anim);
                        }
                    }
                });

                edit_video.setVisibility(View.INVISIBLE);
                edit_video.setClickable(false);

            }
        });
        edit_video.setVisibility(View.VISIBLE);
    }

    private void handlePreviewAudio(TextView fileType) {
        fileType.setText(getResources().getString(R.string.fileType_audio));

        MediaPlayer mp = MediaPlayer.create(PreviewMediaActivity.this, Uri.parse(_managedFile.getFileFullPath()));
        if (mp!=null)
            if  (mp.getDuration() <= MIN_MILISECS_FOR_AUDIO_EDIT) {
                return;
            }

        final ImageButton edit_audio = (ImageButton) findViewById(R.id.editAudio);
        edit_audio.setClickable(true);
        edit_audio.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


                previewThumbnail.setVisibility(View.INVISIBLE);
                previewThumbnail.setClickable(false);
                _previewVideoTrimFile = (ImageButton) findViewById(R.id.playVideoTrimPreview);
                _previewVideoTrimFile.setVisibility(View.VISIBLE);

                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_START_TRIM_IN_MILISEC, 0);
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_END_TRIM_IN_MILISEC, 0);
                startInMili=0;
                endInMili=0;


                FrameLayout waveFrame = (FrameLayout) findViewById(R.id.container);
                waveFrame.setVisibility(View.VISIBLE);


                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, new CustomWaveformFragment(_managedFile.getFileFullPath(),getApplicationContext()))
                        .commit();

                edit_audio.setVisibility(View.INVISIBLE);
                edit_audio.setClickable(false);
            }
        });
        edit_audio.setVisibility(View.VISIBLE);
    }

    private void initVideoProgressPooling(final int stopAtMsec) {
        cancelProgressPooling();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                trimVideoView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!isActive) {
                            cancelProgressPooling();
                            return;
                        }
                        if(trimVideoView.getCurrentPosition() >= stopAtMsec) {
                            trimVideoView.pause();
                            cancelProgressPooling();
                            // Toast.makeText(getApplicationContext(), "Video has PAUSED at: " + trimVideoView.getCurrentPosition(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }, 0, POOLING_INTERVAL_MS);
    }

    private void cancelProgressPooling() {
        if(timer != null) {
            timer.cancel();
        }
        timer = null;
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);

        _imageButton = (ImageButton) findViewById(R.id.preview_thumbnail);
        log(Log.INFO,TAG, "type and path " + fType + "  " + _managedFile.getFileFullPath());


        switch (fType) {
            case AUDIO:
                _imageButton.setImageResource(R.drawable.ringtone_icon);
                break;

            case VIDEO:
            case IMAGE:

                BitmapUtils.execBitMapWorkerTask(_imageButton, fType, _managedFile.getFileFullPath(), false);
                // stretch the uploaded image as it won't stretch because we use a drawable instead that we don't want to stretch
                _imageButton.setPadding(0, 0, 0, 0);
                _imageButton.setScaleType(ImageView.ScaleType.FIT_XY);

                break;
        }



        _imageButton.setVisibility(View.VISIBLE);

    }

    @Override
    protected void onPause() {
        super.onPause();
        closePreview();
        overridePendingTransition(R.anim.no_animation_no_delay, R.anim.slide_out_up);// close drawer animation
        log(Log.INFO,TAG, "onPause()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log(Log.INFO,TAG, "onDestroy()");
    }
    @Override
    public boolean onTouchEvent(MotionEvent event){

        int action = MotionEventCompat.getActionMasked(event);

        switch(action) {
            case (MotionEvent.ACTION_DOWN) :

                _oldPosition = event.getY();
                _moveLength = 0;
                // Log.d(TAG,"Action was DOWN Y is: " + String.valueOf(oldPosition));
                return true;
            case (MotionEvent.ACTION_MOVE) :

                if (event.getY() > (_oldPosition +5)) {
                    _moveLength++;
                    _oldPosition = event.getY();
                }
                //     Log.d(TAG,"Action was MOVE Y is: " + String.valueOf(oldPosition) + " moveLength: " +String.valueOf(_moveLength));
                if (_moveLength > 4) {
                    PreviewMediaActivity.this.finish();
                }

                return true;

            default :
                return super.onTouchEvent(event);
        }
    }

    //endregion

    //region Assisting methods (ReturnFile(), startPreviewStandoutWindow(), ...)
    private void returnFile(MediaFile managedFile) {

        Intent resultIntent = new Intent();
        log(Log.INFO,TAG,"returnFile");

        log(Log.INFO,TAG, "[File selected]: " + managedFile.getFileFullPath() + ". [File Size]: " + MediaFilesUtils.getFileSizeFormat(managedFile.getFileSize()));

        resultIntent.putExtra(RESULT_FILE, managedFile);

        log(Log.INFO,TAG,"End returnFile");

        if (getParent() == null) {
            setResult(Activity.RESULT_OK, resultIntent);
        } else {
            getParent().setResult(Activity.RESULT_OK, resultIntent);
        }

        finish();
    }

    private void startPreviewStandoutWindow(String filepath , MediaFile.FileType fType) {

        AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME, am.getStreamVolume(AudioManager.STREAM_MUSIC));
        log(Log.INFO,TAG, "PreviewStart MUSIC_VOLUME Original" + String.valueOf(am.getStreamVolume(AudioManager.STREAM_MUSIC)));

        // Close previous
        Intent closePrevious = new Intent(getApplicationContext(), PreviewService.class);
        closePrevious.setAction(AbstractStandOutService.ACTION_CLOSE_ALL);
        startService(closePrevious);


        Intent showPreview = new Intent(getApplicationContext(), PreviewService.class);
        showPreview.setAction(AbstractStandOutService.ACTION_PREVIEW);


        switch (fType) {
            case AUDIO:
                showPreview.putExtra(AbstractStandOutService.PREVIEW_AUDIO, filepath);
                showPreview.putExtra(AbstractStandOutService.PREVIEW_VISUAL_MEDIA, "");
                break;

            case VIDEO:
            case IMAGE:
                showPreview.putExtra(AbstractStandOutService.PREVIEW_AUDIO, "");
                showPreview.putExtra(AbstractStandOutService.PREVIEW_VISUAL_MEDIA, filepath);
                break;
        }

        startService(showPreview);

    }

    private void closePreview(){
        //close preview
        Intent closePrevious = new Intent(getApplicationContext(), PreviewService.class);
        closePrevious.setAction(AbstractStandOutService.ACTION_CLOSE_ALL);
        startService(closePrevious);
    }

    public static class CustomWaveformFragment extends WaveformFragment {
        String _filePath;
        Context _Context;

        CustomWaveformFragment(String filePath,Context context){
            _filePath = filePath;
            _Context = context;


            _previewVideoTrimFile.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    if (isPreviewDisplaying) {
                        onPlay(mStartPos);
                        isPreviewDisplaying = false;
                        _previewVideoTrimFile.setImageResource(R.drawable.play_preview_anim);

                    } else {
                        onPlay(mStartPos);
                        isPreviewDisplaying = true;
                        _previewVideoTrimFile.setImageResource(R.drawable.stop_preview_anim);

                    }
                }
            });

        }


        /**
         * Provide path to your audio file.
         *
         * @return
         */
        @Override
        protected String getFileName() {
            return _filePath;
        }

        @Override
        public void updateDisplay() {
            super.updateDisplay();

            try {
                if ( mEndPos == 0) {
                    return;
                }
                startInMili = mWaveformView.pixelsToMillisecs(mStartPos);
                endInMili = mWaveformView.pixelsToMillisecs(mEndPos);

            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        /**
         * Optional - provide list of segments (start and stop values in seconds) and their corresponding colors
         *
         * @return
         */
       /* @Override
        protected List<Segment> getSegments() {
            return Arrays.asList(
                    new Segment(55.2, 55.8, Color.rgb(238, 23, 104)),
                    new Segment(56.2, 56.6, Color.rgb(238, 23, 104)),
                    new Segment(58.4, 59.9, Color.rgb(184, 92, 184)));
        }*/
    }
    //endregion
}



