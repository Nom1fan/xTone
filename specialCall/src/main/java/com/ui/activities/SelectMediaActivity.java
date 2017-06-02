package com.ui.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.*;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.data.objects.ActivityRequestCodes;
import com.data.objects.Constants;
import com.handlers.Handler;
import com.handlers.HandlerFactory;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.mediacallz.app.R;
import com.utils.MediaFileUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.io.File;
import java.util.List;

import com.enums.SpecialMediaType;
import com.files.media.MediaFile;
import com.utils.UtilityFactory;

import static com.crashlytics.android.Crashlytics.log;
import static com.utils.MediaFileUtils.MAX_FILE_SIZE;

/**
 * Created by rony on 29/01/2016.
 */
public class SelectMediaActivity extends Activity implements View.OnClickListener {

    public static final String SPECIAL_MEDIA_TYPE = "SpecialMediaType";
    public static final String DESTINATION_NUMBER = "DestinationNumber";
    public static final String DESTINATION_NAME = "DestinationName";
    public static final String RESULT_ERR_MSG = "ResultErrMsg";
    public static final String RESULT_SPECIAL_MEDIA_TYPE = "ResultSpecialMediaType";
    public static final String RESULT_FILE = "ResultFile";

    private static final String TAG = SelectMediaActivity.class.getSimpleName();
    private String _recordedAudioFilePath;
    private SpecialMediaType specialMediaType;
    private float oldPosition = 0;
    private int moveLength = 0;
    protected MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);


    //region Activity methods (onCreate(), onPause()...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log(Log.INFO, TAG, "onCreate()");
        Intent intent = getIntent();
        specialMediaType = (SpecialMediaType) intent.getSerializableExtra(SPECIAL_MEDIA_TYPE);

        initializeSelectMediaUI();

    }

    private void initializeSelectMediaUI() {

        setContentView(R.layout.select_media);
        ImageView button1 = (ImageView) findViewById(R.id.mc_icon);
        button1.setOnClickListener(this);
        TextView mediaType = (TextView) findViewById(R.id.selectMediaType);

        if (SpecialMediaType.CALLER_MEDIA == specialMediaType)
            mediaType.setText(R.string.select_caller_media_title);
        else if (SpecialMediaType.PROFILE_MEDIA == specialMediaType)
            mediaType.setText(R.string.select_profile_media_title);
//        else if(ActivityRequestCodes.SELECT_DEFAULT_PROFILE_MEDIA == specialMediaType)
//            mediaType.setText(R.string.select_default_profile_media_title);

        ImageButton videoBtn = (ImageButton) findViewById(R.id.selectVideo);
        videoBtn.setOnClickListener(this);

        ImageButton imageBtn = (ImageButton) findViewById(R.id.selectImage);
        imageBtn.setOnClickListener(this);

        ImageButton recordVideoBtn = (ImageButton) findViewById(R.id.recordVideo);
        recordVideoBtn.setOnClickListener(this);

        ImageButton takePictureBtn = (ImageButton) findViewById(R.id.takePicture);
        takePictureBtn.setOnClickListener(this);

        ImageButton audioBtn = (ImageButton) findViewById(R.id.audio);
        audioBtn.setOnClickListener(this);

        ImageButton recordAudioBtn = (ImageButton) findViewById(R.id.recordAudio);
        recordAudioBtn.setOnClickListener(this);


        TextView imageVideoTextView = (TextView) findViewById(R.id.image_video_textview);
        imageVideoTextView.setOnClickListener(this);

        TextView audioTextView = (TextView) findViewById(R.id.audio_textview);
        audioTextView.setOnClickListener(this);

        TextView takePicTextView = (TextView) findViewById(R.id.take_picture_textview);
        takePicTextView.setOnClickListener(this);

        TextView recordVideoTextView = (TextView) findViewById(R.id.record_video_textview);
        recordVideoTextView.setOnClickListener(this);

        TextView recordAudioTextView = (TextView) findViewById(R.id.record_audio_textview);
        recordAudioTextView.setOnClickListener(this);

        TextView galleryTexView = (TextView) findViewById(R.id.mc_gallery_textview);
        ImageButton mcContentStoreBtn = (ImageButton) findViewById(R.id.mc_contentstore_btn);

        mcContentStoreBtn.setOnClickListener(this);
        galleryTexView.setOnClickListener(this);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        log(Log.INFO, TAG, "onActivityResult");
        if (resultCode == RESULT_OK) {
            try {
                Handler requestHandler = HandlerFactory.getInstance().getHandler(TAG, requestCode);
                if (requestHandler != null)
                    requestHandler.handle(SelectMediaActivity.this, data, SelectMediaActivity.this, specialMediaType);
            } catch (Exception e) {
                e.printStackTrace();
                log(Log.ERROR, TAG, e.getMessage());
            }
        }
    }

    private void showInvalidFileOrPathToast() {
        UI_Utils.callToast(getResources().getString(R.string.file_invalid),
                Color.RED, Toast.LENGTH_LONG, getApplicationContext());
    }

    private void startPreviewActivity(String filepath) {

        File file = new File(filepath);

        if (!file.exists()) {
            showInvalidFileOrPathToast();
            finish();
        }

        MediaFile mediaFile = new MediaFile(file);

        if (mediaFile.getSize() > MAX_FILE_SIZE) {
            String errMsg = String.format(getResources().getString(R.string.file_over_max_size),
                    mediaFileUtils.getFileSizeFormat(MAX_FILE_SIZE));

            UI_Utils.callToast(errMsg, Color.RED, Toast.LENGTH_LONG, getApplicationContext());
            finish();
        }

        Intent previewIntentActivity = new Intent(this, PreviewMediaActivity.class);
        previewIntentActivity.putExtra(PreviewMediaActivity.MANAGED_MEDIA_FILE, mediaFile);
        previewIntentActivity.putExtra(SPECIAL_MEDIA_TYPE, specialMediaType);
        startActivityForResult(previewIntentActivity, ActivityRequestCodes.PREVIEW_MEDIA);
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.no_animation_no_delay, R.anim.slide_out_up);// close drawer animation
        log(Log.INFO, TAG, "onPause()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log(Log.INFO, TAG, "onDestroy()");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = MotionEventCompat.getActionMasked(event);

        switch (action) {
            case (MotionEvent.ACTION_DOWN):

                oldPosition = event.getY();
                moveLength = 0;
                // Log.d(TAG,"Action was DOWN Y is: " + String.valueOf(oldPosition));
                return true;
            case (MotionEvent.ACTION_MOVE):

                if (event.getY() > (oldPosition + 5)) {
                    moveLength++;
                    oldPosition = event.getY();
                }
                //     Log.d(TAG,"Action was MOVE Y is: " + String.valueOf(oldPosition) + " moveLength: " +String.valueOf(moveLength));
                if (moveLength > 4) {
                    SelectMediaActivity.this.finish();
                }

                return true;

            default:
                return super.onTouchEvent(event);
        }
    }
    //endregion

    //region Assisting methods (onClick(), takePicture(), ...)
    @Override
    public void onClick(View v) {

        int id = v.getId();

        if (id == R.id.mc_icon) {
            SelectMediaActivity.this.finish();
        } else if (id == R.id.selectVideo || id == R.id.image_video_textview || id == R.id.selectImage) {

            openVideoAndImageMediaPath();

        } else if (id == R.id.audio || id == R.id.audio_textview) {

            openAudioMediaPath();
        } else if (id == R.id.recordVideo || id == R.id.record_video_textview) {

            recordVideo();
        } else if (id == R.id.takePicture || id == R.id.take_picture_textview) {

            takePicture();
        } else if (id == R.id.recordAudio || id == R.id.record_audio_textview) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                ActivityCompat.requestPermissions(SelectMediaActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, Constants.MY_PERMISSIONS_AUDIO_RECORDING);
            else
                initialRecordAudioProcess();

        } else if (id == R.id.mc_contentstore_btn || id == R.id.mc_gallery_textview) {

            startContentStore();
        }

    }

    private void initialRecordAudioProcess() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final TextView content = new TextView(this);
        content.setText(R.string.record_audio_desc);

        builder.setTitle(R.string.record_audio_title)
                .setView(content)
                .setPositiveButton(R.string.start, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        recordAudio();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        dialog.cancel();

                    }
                });

        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_AUDIO_RECORDING: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    initialRecordAudioProcess();
                else
                    showRationale();
                break;
            }
        }
    }

    private void showRationale() {


        if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            // user also CHECKED "never ask again"
            // you can either enable some fall back,
            // disable features of your app
            // or open another dialog explaining
            // again the permission and directing to
            // the app setting
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            TextView content = new TextView(this);
            content.setText(R.string.permission_a_must);

            builder.setTitle(R.string.permission_denied)
                    .setView(content)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            dialog.cancel();

                        }
                    })
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivityForResult(intent, 1985);
                        }
                    });
            builder.create().show();
        } else {
            // user did NOT check "never ask again"
            // this is a good place to explain the user
            // why you need the permission and ask if he wants
            // to accept it (the rationale)
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            TextView content = new TextView(this);
            content.setText(R.string.permission_a_must);

            builder.setTitle(R.string.permission_denied)
                    .setView(content)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            dialog.cancel();

                        }
                    })
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ActivityCompat.requestPermissions(SelectMediaActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, Constants.MY_PERMISSIONS_AUDIO_RECORDING);
                        }
                    });
            builder.create().show();

        }

    }

    private void startContentStore() {
        Intent i = new Intent(SelectMediaActivity.this, HomeActivity.class);
        // Using PREVIEW_MEDIA as request code since the content store goes through the preview activity and then returns the result
        startActivityForResult(i, ActivityRequestCodes.PREVIEW_MEDIA);
    }

    private void openVideoAndImageMediaPath() {

        // Create the ACTION_GET_CONTENT Intent
        // Intent intent = FileUtils.createGetContentIntent("image/*, video/*"); // // TODO rony: 31/01/2016 we may need to change it to "*/*" as it will give us filechooser support and more ways to open files. also i think in android 5 or 6 it's not supported the ("image/* , video/*") but not sure
        Intent intent = FileUtils.createGetContentIntent("*/*");
        Intent chooserIntent = Intent.createChooser(intent, "Select Media File");
        startActivityForResult(chooserIntent, ActivityRequestCodes.FIlE_CHOOSER);

    }

    private void openAudioMediaPath() {

        if (SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_HISTORY_EXIST))
            openAudioMenu();
        else {
            final Intent intent = FileUtils.createGetContentIntent("audio/*");
            Intent chooserIntent = Intent.createChooser(intent, "Select Audio File");
            startActivityForResult(chooserIntent, ActivityRequestCodes.FIlE_CHOOSER);
        }
    }

    private void openAudioMenu() {
        ImageButton profile = (ImageButton) findViewById(R.id.audio);
        //Creating the instance of PopupMenu
        PopupMenu popup = new PopupMenu(SelectMediaActivity.this, profile);
        //Inflating the Popup using xml file
        popup.getMenuInflater().inflate(R.menu.popup_audio_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {

                log(Log.INFO, TAG, String.valueOf(item.getItemId()));

                switch (item.getItemId()) {
                    case R.id.device_audio:
                        final Intent intent = FileUtils.createGetContentIntent("audio/*");
                        Intent chooserIntent = Intent.createChooser(intent, "Select Audio File");
                        startActivityForResult(chooserIntent, ActivityRequestCodes.FIlE_CHOOSER);
                        break;

                    case R.id.audio_history:

                        Intent audioIntent = new Intent(SelectMediaActivity.this, AudioFilesHistoryActivity.class);
                        audioIntent.putExtra(SelectMediaActivity.SPECIAL_MEDIA_TYPE, specialMediaType);
                        startActivityForResult(audioIntent, ActivityRequestCodes.PREVIEW_MEDIA);

                        break;

                }
                return true;
            }
        });

        popup.show();
    }


    private void recordVideo() {
        // Determine Uri of camera image to save.
        String fname = "MyVideo_" + System.currentTimeMillis() + ".mp4";
        File sdVideoMainDirectory = new File(Constants.HISTORY_FOLDER, fname);

        sdVideoMainDirectory.delete();

        SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.SELF_VIDEO_IMAGE_URI, Uri.fromFile(sdVideoMainDirectory).toString());

        final Intent videoIntent = new Intent(
                MediaStore.ACTION_VIDEO_CAPTURE);
        videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(sdVideoMainDirectory));
        videoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30); // set video recording interval
        videoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0); // set the video image quality to low
        videoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 15000);

        startActivityForResult(videoIntent, ActivityRequestCodes.REQUEST_CAMERA); // // TODO rony: 31/01/2016 see native camera opens and not other weird different cameras
    }

    private void takePicture() {

        // Determine Uri of camera image to save.
        String fname = "MyImage_" + System.currentTimeMillis() + ".jpeg";
        File sdImageMainDirectory = new File(Constants.HISTORY_FOLDER, fname);
        sdImageMainDirectory.delete();
        SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.SELF_VIDEO_IMAGE_URI, Uri.fromFile(sdImageMainDirectory).toString());
        // Camera.
        final Intent captureIntent = new Intent(
                android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(
                captureIntent, 0);
        Intent cameraIntent = new Intent();
        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName,
                    res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(sdImageMainDirectory));
            cameraIntent = intent;
        }

        startActivityForResult(cameraIntent, ActivityRequestCodes.REQUEST_CAMERA);  // // TODO rony: 31/01/2016 see native camera opens and not other weird different cameras

    }

    public void recordAudio() {

        String fname = "MyAudioRecording_" + System.currentTimeMillis() + ".m4a";
        File sdAudioFile = new File(Constants.AUDIO_HISTORY_FOLDER, fname);
        SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_HISTORY_EXIST, true);

        sdAudioFile.delete();
        _recordedAudioFilePath = sdAudioFile.getAbsolutePath();


        final MediaRecorder recorder = new MediaRecorder();
        ContentValues values = new ContentValues(3);
        values.put(MediaStore.MediaColumns.TITLE, fname);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(_recordedAudioFilePath);


        try {
            recorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final ProgressDialog mProgressDialog = new ProgressDialog(SelectMediaActivity.this);  // // TODO rony: 31/01/2016  add seconds to the progress of the recording
        mProgressDialog.setTitle(getResources().getString(R.string.recording));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setButton(getResources().getString(R.string.stop_recording), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mProgressDialog.dismiss();
                recorder.stop();
                recorder.release();

                startPreviewActivity(_recordedAudioFilePath);

            }
        });

        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface p1) {
                recorder.stop();
                recorder.release();
            }
        });
        try {
            recorder.start();
            mProgressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
