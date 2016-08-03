package com.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import com.mediacallz.app.R;
import com.semantive.waveformandroid.waveform.WaveformFragment;
import com.services.AbstractStandOutService;
import com.services.PreviewService;
import com.utils.BitmapUtils;
import com.utils.SharedPrefUtils;

import FilesManager.FileManager;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by rony on 29/01/2016.
 */
public class PreviewMediaActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String MANAGED_MEDIA_FILE = "MANAGED_MEDIA_FILE";
    public static final String RESULT_FILE = "RESULT_FILE";

    private static final String TAG = PreviewMediaActivity.class.getSimpleName();
    private FileManager _managedFile;
    private float _oldPosition =0;
    private int _moveLength = 0;
    private int _SMTypeCode;
    private boolean _isPreview = false;
    private ImageButton _previewFile;
    private ImageButton _imageButton;
    private FileManager.FileType fType;
    private final int MIN_MILISECS_FOR_AUDIO_EDIT = 3000;
    protected int startInMili;
    protected int endInMili;

    //region Activity methods (onCreate(), onPause()...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log(Log.INFO,TAG, "onCreate()");
        Intent intent = getIntent();
        _managedFile = (FileManager) intent.getSerializableExtra(MANAGED_MEDIA_FILE);
        _SMTypeCode = intent.getIntExtra(SelectMediaActivity.SPECIAL_MEDIA_TYPE, 1);
        initializePreviewMediaUI();

    }

    private void initializePreviewMediaUI() {

        closePreview();

        setContentView(R.layout.preview_before_upload);

        TextView fileType = (TextView) findViewById(R.id.upload_file_type);
        TextView fileName = (TextView) findViewById(R.id.upload_file_name);
        fileName.setText(FileManager.getFileNameWithExtension(_managedFile.getFileFullPath()));
        fType = _managedFile.getFileType();

        ImageButton upload = (ImageButton) findViewById(R.id.upload_btn);
        upload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closePreview();

                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_START_TRIM_IN_MILISEC, startInMili);
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_END_TRIM_IN_MILISEC, endInMili);
                startInMili=0;
                endInMili=0;

                getSupportFragmentManager().popBackStack(); // closes the audio editor fragment in case it is still playing audio

                returnFile(_managedFile);
            }
        });

        ImageButton cancel = (ImageButton) findViewById(R.id.cancel_btn);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closePreview();
                getSupportFragmentManager().popBackStack(); // closes the audio editor fragment in case it is still playing audio
                finish();
            }
        });

        _previewFile = (ImageButton) findViewById(R.id.playPreview);
        _previewFile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (_isPreview) {
                    closePreview();
                    _isPreview = false;
                    _previewFile.setImageResource(R.drawable.play_preview_anim);
                } else {
                    startPreviewStandoutWindow(_managedFile.getFileFullPath() , fType);
                    _isPreview = true;
                    _previewFile.setImageResource(R.drawable.stop_preview_anim);
                }
            }
        });

        switch (fType) {
            case AUDIO:
                fileType.setText(getResources().getString(R.string.fileType_audio));

                MediaPlayer mp = MediaPlayer.create(PreviewMediaActivity.this, Uri.parse(_managedFile.getFileFullPath()));
                if  (mp.getDuration() <= MIN_MILISECS_FOR_AUDIO_EDIT) {
                    break;
                }

                final ImageButton edit_audio = (ImageButton) findViewById(R.id.editAudio);
                edit_audio.setClickable(true);
                edit_audio.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {


                        _previewFile.setVisibility(View.INVISIBLE);
                        _previewFile.setClickable(false);
                        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_START_TRIM_IN_MILISEC , 0);
                        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_END_TRIM_IN_MILISEC , 0);
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



                break;

            case VIDEO:
                fileType.setText(getResources().getString(R.string.fileType_video));
                break;

            case IMAGE:

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
                    _previewFile.setVisibility(View.INVISIBLE);
                    _previewFile.setClickable(false);
                    TextView rotateTextview = (TextView) findViewById(R.id.rotate_textview);
                    rotateTextview.setVisibility(View.VISIBLE);
                }
                break;
        }


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

    //region Assisting methods (onClick(), takePicture(), ...)

    private void returnFile(FileManager managedFile) {

        Intent resultIntent = new Intent();
        log(Log.INFO,TAG,"returnFile");

        log(Log.INFO,TAG, "[File selected]: " + managedFile.getFileFullPath() + ". [File Size]: " + FileManager.getFileSizeFormat(managedFile.getFileSize()));

        resultIntent.putExtra(SelectMediaActivity.SPECIAL_MEDIA_TYPE, _SMTypeCode);
        resultIntent.putExtra(RESULT_FILE, managedFile);

        log(Log.INFO,TAG,"End returnFile");

        if (getParent() == null) {
            setResult(Activity.RESULT_OK, resultIntent);
        } else {
            getParent().setResult(Activity.RESULT_OK, resultIntent);
        }

        finish();
    }

    @Override
    public void onClick(View v) {

    }

    private void startPreviewStandoutWindow(String filepath , FileManager.FileType fType) {

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

    public class CustomWaveformFragment extends WaveformFragment {
        String _filePath;
        Context _Context;

        CustomWaveformFragment(String filePath,Context context){
            _filePath = filePath;
            _Context = context;
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
}

    //endregion



