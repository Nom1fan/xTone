package com.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.data_objects.ActivityRequestCodes;
import com.data_objects.Constants;
import com.handlers.Handler;
import com.handlers.HandlerFactory;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.mediacallz.app.R;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.io.File;
import java.util.List;

import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import Exceptions.InvalidDestinationNumberException;
import FilesManager.FileManager;

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
    private int SMTypeCode;
    private String _destName = "";
    private String _destPhoneNumber = "";
    private float oldPosition = 0;
    private int moveLength= 0;
    private WebView mwebView;
    private ProgressDialog _progDialog;
    private boolean _isInWebView = false;

    //region Activity methods (onCreate(), onPause()...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.log(Log.INFO,TAG, "onCreate()");
        Intent intent = getIntent();
        _destPhoneNumber = intent.getStringExtra(DESTINATION_NUMBER);
        _destName = intent.getStringExtra(DESTINATION_NAME);
        SMTypeCode = intent.getIntExtra(SPECIAL_MEDIA_TYPE, 1);

        try {
            //TODO Move this check to MainActivity
            checkDestinationNumber();
        } catch (InvalidDestinationNumberException e) {
            e.printStackTrace();
            UI_Utils.callToast(getResources().getString(R.string.destnumber_invalid), Color.RED, Toast.LENGTH_LONG, getApplicationContext());
            finish();
        }
        initializeSelectMediaUI();

    }

    private void initializeSelectMediaUI() {

        setContentView(R.layout.select_media);
        _isInWebView = false;
        ImageView button1 = (ImageView) findViewById(R.id.mc_icon);
        button1.setOnClickListener(this);
        TextView mediaType = (TextView) findViewById(R.id.selectMediaType);

        if (ActivityRequestCodes.SELECT_CALLER_MEDIA == SMTypeCode)
            mediaType.setText(R.string.select_caller_media_title);
        else
            mediaType.setText(R.string.select_profile_media_title);

        TextView nameOrPhone = (TextView) findViewById(R.id.contactNameOrNumber);

        if (_destName.isEmpty())
            nameOrPhone.setText(_destPhoneNumber);
        else
            nameOrPhone.setText(_destName);

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

        ImageButton mediacallzBtn = (ImageButton) findViewById(R.id.mediacallzBtn);
        mediacallzBtn.setOnClickListener(this);

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
        galleryTexView.setOnClickListener(this);


    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && _isInWebView) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (mwebView != null) {
                        if (mwebView.canGoBack()) {
                            mwebView.goBack();
                        } else
                            initializeSelectMediaUI();
                    }

                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Crashlytics.log(Log.INFO, TAG, "onActivityResult");
        if (resultCode == RESULT_OK) {
            try {
                Handler requestHandler = HandlerFactory.getInstance().getHandler(TAG, requestCode);
                if(requestHandler!=null)
                    requestHandler.handle(SelectMediaActivity.this, data, SelectMediaActivity.this);
            } catch (Exception e) {
                e.printStackTrace();
                Crashlytics.log(Log.ERROR, TAG, e.getMessage());
            }
        }
    }

    private void showInvalidFileOrPathToast() {
        UI_Utils.callToast(getResources().getString(R.string.file_invalid),
                Color.RED, Toast.LENGTH_LONG, getApplicationContext());
    }

    private void startPreviewActivity(String filepath) {

        FileManager managedFile;
        try {
            managedFile = new FileManager(filepath);

            Intent previewIntentActivity = new Intent(this, PreviewMediaActivity.class);
            previewIntentActivity.putExtra(PreviewMediaActivity.MANAGED_MEDIA_FILE, managedFile);
            previewIntentActivity.putExtra(SPECIAL_MEDIA_TYPE, SMTypeCode);
            startActivityForResult(previewIntentActivity, ActivityRequestCodes.PREVIEW_MEDIA);

        } catch(FileExceedsMaxSizeException e) {
            e.printStackTrace();
            String errMsg = String.format(getResources().getString(R.string.file_over_max_size),
                    FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));

            UI_Utils.callToast(errMsg, Color.RED, Toast.LENGTH_LONG, getApplicationContext());
            finish();

        } catch (FileMissingExtensionException | FileDoesNotExistException | FileInvalidFormatException e) {
            e.printStackTrace();
            showInvalidFileOrPathToast();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.no_animation_no_delay, R.anim.slide_out_up);// close drawer animation
        Crashlytics.log(Log.INFO,TAG, "onPause()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Crashlytics.log(Log.INFO,TAG, "onDestroy()");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        int action = MotionEventCompat.getActionMasked(event);

        switch(action) {
            case (MotionEvent.ACTION_DOWN) :

                oldPosition = event.getY();
                moveLength = 0;
               // Log.d(TAG,"Action was DOWN Y is: " + String.valueOf(oldPosition));
                return true;
            case (MotionEvent.ACTION_MOVE) :

                if (event.getY() > (oldPosition+5)) {
                    moveLength++;
                    oldPosition = event.getY();
                }
               //     Log.d(TAG,"Action was MOVE Y is: " + String.valueOf(oldPosition) + " moveLength: " +String.valueOf(moveLength));
                    if (moveLength > 4) {
                        SelectMediaActivity.this.finish();
                    }

                return true;

            default :
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
        }
        else if (id == R.id.selectVideo || id == R.id.image_video_textview || id == R.id.selectImage) {

            openVideoAndImageMediaPath();

        }else if (id == R.id.audio || id == R.id.audio_textview) {

            openAudioMediaPath();
        }
        else if (id == R.id.recordVideo || id == R.id.record_video_textview) {

            recordVideo();
        }
        else if (id == R.id.takePicture || id == R.id.take_picture_textview) {

            takePicture();
        }
        else if (id == R.id.recordAudio || id == R.id.record_audio_textview) {

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


                        }
                    });

            builder.create().show();

        }
        else {
            if (id == R.id.mediacallzBtn || id == R.id.mc_gallery_textview) {

                mediacallzGalleryWebView();
                _isInWebView = true;
            }
        }
    }

    private void mediacallzGalleryWebView(){

        mwebView = new WebView(this);
        mwebView.setWebChromeClient(new WebChromeClient());
        WebViewClient client = new ChildBrowserClient();
        mwebView.setWebViewClient(client);
        WebSettings settings = mwebView.getSettings();
        settings.setJavaScriptEnabled(true);
        mwebView.setInitialScale(1);
        mwebView.getSettings().setUseWideViewPort(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setBuiltInZoomControls(true);
        settings.setPluginState(WebSettings.PluginState.ON);
        settings.setDomStorageEnabled(true);
        mwebView.loadUrl(Constants.MEDIACALLZ_CONTENT_STORE_URL);
        mwebView.setInitialScale(0);
        mwebView.requestFocus();
        mwebView.requestFocusFromTouch();
        mwebView.setEnabled(true);

        RelativeLayout layout = new RelativeLayout(this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(400,400);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);

        layout.addView(mwebView);


        setContentView(layout);
    }

    public class ChildBrowserClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {


         //   boolean isURL = true;
            String extension = MimeTypeMap.getFileExtensionFromUrl(url);
            if (extension != null) {
                    if (FileManager.isExtensionValid(extension)) {

                        try {
                            final DownloadManager mdDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                            DownloadManager.Request request = new DownloadManager.Request(
                                    Uri.parse(url));

                            final File destinationFile = new File(
                                    Constants.HISTORY_FOLDER, System.currentTimeMillis() + "." + extension);
                            request.setDescription("MediaCallz " + getResources().getString(R.string.downloading));
                            request.allowScanningByMediaScanner();
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            request.setDestinationUri(Uri.fromFile(destinationFile));
                            long downloadId = mdDownloadManager.enqueue(request);

                                downloadFileFromWebView downloadTask = new downloadFileFromWebView(downloadId, destinationFile.getAbsolutePath());
                                downloadTask.execute();

                            Crashlytics.log(Log.INFO,TAG, "destinationfiles: " + Constants.HISTORY_FOLDER + String.valueOf(System.currentTimeMillis() + "." + extension));

                    }catch (Exception e)
                        {
                            UI_Utils.callToast(getResources().getString(R.string.oops_try_again), Color.RED, Toast.LENGTH_LONG, getApplicationContext());
                            Crashlytics.log(Log.INFO,TAG, "shouldOverrideUrlLoading exception: " + (e.getMessage()!=null ? e.getMessage() : e));
                        }

                        }
            }
            return false;
        }
    }
    private void openVideoAndImageMediaPath() {

        // Create the ACTION_GET_CONTENT Intent
        // Intent intent = FileUtils.createGetContentIntent("image/*, video/*"); // // TODO rony: 31/01/2016 we may need to change it to "*/*" as it will give us filechooser support and more ways to open files. also i think in android 5 or 6 it's not supported the ("image/* , video/*") but not sure
        Intent intent = FileUtils.createGetContentIntent("*/*");
        Intent chooserIntent = Intent.createChooser(intent, "Select Media File");
        startActivityForResult(chooserIntent, ActivityRequestCodes.FIlE_CHOOSER);

    }

    private void openAudioMediaPath() {
        // Create the ACTION_GET_CONTENT Intent
        final Intent intent = FileUtils.createGetContentIntent("audio/*");
        Intent chooserIntent = Intent.createChooser(intent, "Select Audio File");
        startActivityForResult(chooserIntent, ActivityRequestCodes.FIlE_CHOOSER);
    }

    private void recordVideo() {
        // Determine Uri of camera image to save.
        String fname =  "MyVideo_"+System.currentTimeMillis()+".mp4";
        File sdVideoMainDirectory = new File(Constants.HISTORY_FOLDER, fname);

        sdVideoMainDirectory.delete();

        SharedPrefUtils.setString(getApplicationContext(),SharedPrefUtils.GENERAL,SharedPrefUtils.SELF_VIDEO_IMAGE_URI,Uri.fromFile(sdVideoMainDirectory).toString());

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
        String fname = "MyImage_"+System.currentTimeMillis()+".jpeg";
        File sdImageMainDirectory = new File(Constants.HISTORY_FOLDER, fname);
        sdImageMainDirectory.delete();
        SharedPrefUtils.setString(getApplicationContext(),SharedPrefUtils.GENERAL,SharedPrefUtils.SELF_VIDEO_IMAGE_URI,Uri.fromFile(sdImageMainDirectory).toString());
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

    private void checkDestinationNumber() throws InvalidDestinationNumberException {

        if(_destPhoneNumber == null)
            throw new InvalidDestinationNumberException();
        if(_destPhoneNumber.equals("") || _destPhoneNumber.length() < 10)
            throw new InvalidDestinationNumberException();
    }

    public void recordAudio() {

        String fname = "MyAudioRecording_"+System.currentTimeMillis()+".mp3";
        File sdAudioFile = new File(Constants.HISTORY_FOLDER, fname);

        sdAudioFile.delete();
        _recordedAudioFilePath = sdAudioFile.getAbsolutePath();


        final MediaRecorder recorder = new MediaRecorder();
        ContentValues values = new ContentValues(3);
        values.put(MediaStore.MediaColumns.TITLE, fname);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setOutputFile(_recordedAudioFilePath);


        try {
            recorder.prepare();
        } catch (Exception e){
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
      try{
        recorder.start();
        mProgressDialog.show();}
      catch (Exception e){
          e.printStackTrace();
      }

    }

    private class downloadFileFromWebView extends AsyncTask<Void, Integer, Void> {

        private final String TAG = SelectMediaActivity.class.getSimpleName();

        private downloadFileFromWebView _instance = this;
        private Long _iD;
        private DownloadManager _downloadManager;
        private String _filePath;
        private Boolean _downloading = false;
        private Cursor cursor;
        public downloadFileFromWebView(long downloadId , String filepath) {

            _downloadManager =  (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            _iD = downloadId;
            _filePath = filepath;
            _downloading=true;
            _instance = this;

        }

        @Override
        protected void onPreExecute() {

            String cancel = getResources().getString(R.string.cancel);

            _progDialog = new ProgressDialog(SelectMediaActivity.this);
            _progDialog.setIndeterminate(false);
            _progDialog.setCancelable(false);
            _progDialog.setTitle(getResources().getString(R.string.downloading));
            _progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            _progDialog.setProgress(0);
            _progDialog.setMax(100);
            _progDialog.setButton(DialogInterface.BUTTON_NEGATIVE, cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    _instance.cancel(true);

                    Crashlytics.log(Log.INFO,TAG , " cancel start");
                    _downloadManager =  (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(_iD);

                    Cursor cursor = _downloadManager.query(q);

                    cursor.moveToFirst();

                    _downloadManager.remove(_iD);

                    _downloading=false;

                    cursor.close();

                    if(mwebView!=null)
                        if (mwebView.canGoBack()) {
                            mwebView.goBack();

                        }

                    Crashlytics.log(Log.INFO,TAG , " cancel end");

                }
            });

            _progDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            // Worker thread
            new Thread(new Runnable() { // TODO AsyncTask

                @Override
                public void run() {


                    while (_downloading) {

                        try {
                            DownloadManager.Query q = new DownloadManager.Query();
                            q.setFilterById(_iD);

                            cursor = _downloadManager.query(q);

                            cursor.moveToFirst();
                            int bytes_downloaded = cursor.getInt(cursor
                                    .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                            if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                                _downloading = false;
                            }

                            //  _progDialog.setMax(bytes_total);

                            if (bytes_total > FileManager.MAX_FILE_SIZE) {

                                String errMsg = String.format(getResources().getString(R.string.file_over_max_size),
                                        FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));

                                UI_Utils.callToast(errMsg, Color.RED, Toast.LENGTH_LONG, getApplicationContext());

                                _downloadManager =  (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                                _downloadManager.remove(_iD);

                                _downloading=false;

                                if (cursor!=null)
                                    cursor.close();

                                _progDialog.dismiss();

                                if(mwebView!=null)
                                    if (mwebView.canGoBack()) {
                                        mwebView.goBack();

                                    }


                            }

                            int dl_progress = (int) ((bytes_downloaded * 100l) / bytes_total);


                            //   publishProgress(dl_progress);


                            _progDialog.setProgress(dl_progress);

                            if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {

                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                if (_progDialog != null && _progDialog.isShowing()) {
                                    _progDialog.dismiss();
                                }

                                startPreviewActivity(_filePath);
                                _downloading = false;
                            }

                            // Log.d(TAG, statusMessage(cursor)); // for debug purposes only
                            if (cursor != null)
                                cursor.close();

                        }
                        catch(Exception e){

                            _downloading=false;
                            e.printStackTrace();
                            Crashlytics.log(Log.ERROR,TAG, "Failed:" + e.getMessage());

                        }
                    }
                }
            }).start();

            return null;
        }

        //endregion

      /*  @Override
        protected void onProgressUpdate(Integer... progress) {

            if (_progDialog != null) {
                {

                    _progDialog.setProgress(progress[0]);


                }
            }
        }*/

    }

    public int getSMTypeCode() {
        return SMTypeCode;
    }

}
