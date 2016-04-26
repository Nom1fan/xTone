package com.ui.activities;

import android.app.Activity;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.data_objects.ActivityRequestCodes;
import com.data_objects.Constants;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.mediacallz.app.R;
import com.utils.UI_Utils;

import java.io.File;
import java.util.List;

import DataObjects.SpecialMediaType;
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
    private Uri _outputFileUri;
    private String _recordedAudioFilePath;
    private int SMTypeCode;
    private String _destPhoneNumber = "";
    private float oldPosition =0;
    private int moveLength= 0;
    private WebView mwebView;
    private ProgressBar mProgressbar;  // TODO progressDialog , Try it out
    private TextView mprogressTextView;
    private long mDownloadId;
    private final String MEDIACALLZ_GALLERY_URL = "http://download.wavetlan.com/SVV/Media/HTTP/http-mp4.htm"; // TODO Place it in Constants

    //region Activity methods (onCreate(), onPause()...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        Intent intent = getIntent();
        setContentView(R.layout.select_media);

        ImageButton button1 = (ImageButton) findViewById(R.id.back);
        button1.setOnClickListener(this);
        TextView mediaType = (TextView) findViewById(R.id.selectMediaType);

        _destPhoneNumber = intent.getStringExtra(DESTINATION_NUMBER);
        String _destName = intent.getStringExtra(DESTINATION_NAME);
        SMTypeCode = intent.getIntExtra(SPECIAL_MEDIA_TYPE, 1);

        if (ActivityRequestCodes.SELECT_CALLER_MEDIA == SMTypeCode)
            mediaType.setText(R.string.select_caller_media_title);
        else
            mediaType.setText(R.string.select_profile_media_title);

        TextView nameOrPhone = (TextView) findViewById(R.id.contactNameOrNumber);

        if (_destName.isEmpty())
            nameOrPhone.setText(_destPhoneNumber);
        else
            nameOrPhone.setText(_destName);

        ImageButton videoBtn = (ImageButton) findViewById(R.id.video_or_image);
        videoBtn.setOnClickListener(this);

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

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                if(mwebView!=null)
                    {if (mwebView.canGoBack()) {
                        mwebView.goBack();

                    }
                    else
                    finish();}  // TODO get back to the Select Media Activity
                   return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i(TAG, "onActivityResult");
        if (resultCode == RESULT_OK) {
            Log.i(TAG, "requestCode: " + requestCode);
            if (requestCode == ActivityRequestCodes.SELECT_CALLER_MEDIA) {

                SpecialMediaType specialMediaType = SpecialMediaType.CALLER_MEDIA;
                extractAndReturnFile(data, specialMediaType);
            } else if (requestCode == ActivityRequestCodes.SELECT_PROFILE_MEDIA) {

                SpecialMediaType specialMediaType = SpecialMediaType.PROFILE_MEDIA;
                extractAndReturnFile(data, specialMediaType);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
        overridePendingTransition(R.anim.no_animation, R.anim.slide_out_up);// close drawer animation
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
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

        if (id == R.id.back) {
            SelectMediaActivity.this.finish();
        }
        else if (id == R.id.video_or_image) {

            openVideoAndImageMediapath(SMTypeCode);

        } else if (id == R.id.audio) {

            openAudioMediapath(SMTypeCode);
        }
        else if (id == R.id.recordVideo) {

            RecordVideo(SMTypeCode);
        }
        else if (id == R.id.takePicture) {

            takePicture(SMTypeCode);
        }
        else if (id == R.id.recordAudio) {

            recordAudio();
        }
        else {
            if (id == R.id.mediacallzBtn) {

                mediacallzGalleryWebView();

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
        mwebView.loadUrl(MEDIACALLZ_GALLERY_URL);
        mwebView.setId(5);
        mwebView.setInitialScale(0);
        mwebView.requestFocus();
        mwebView.requestFocusFromTouch();
        mwebView.setEnabled(true);

        RelativeLayout layout = new RelativeLayout(this);
        mProgressbar = new ProgressBar(getApplicationContext(),null,android.R.attr.progressBarStyleLarge);
        mProgressbar.setIndeterminate(true);
        mProgressbar.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(400,400);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);

        mprogressTextView = new TextView(getApplicationContext());
        RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(400,400);
        params1.addRule(RelativeLayout.CENTER_IN_PARENT);
        params1.addRule(RelativeLayout.BELOW, mProgressbar.getId());
        params1.setMargins(0,300,0,300);
        mprogressTextView.setVisibility(View.VISIBLE);

        layout.addView(mProgressbar, params);
        layout.addView(mprogressTextView, params1);
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
                            final long downloadId = mdDownloadManager.enqueue(request);

                            //mDownloadId = downloadId;

                            new Thread(new Runnable() { // TODO AsyncTask

                                @Override
                                public void run() {

                                    boolean downloading = true;

                                    while (downloading) {

                                        DownloadManager.Query q = new DownloadManager.Query();
                                        q.setFilterById(downloadId);

                                        Cursor cursor = mdDownloadManager.query(q);

                                        cursor.moveToFirst();
                                        int bytes_downloaded = cursor.getInt(cursor
                                                .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                        int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                                        if (bytes_total > FileManager.MAX_FILE_SIZE) {

                                            String errMsg = String.format(getResources().getString(R.string.file_over_max_size),
                                                    FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));

                                            UI_Utils.callToast(errMsg, Color.RED, Toast.LENGTH_LONG, getApplicationContext());


                                            disableProgressBar();
                                            mdDownloadManager.remove(downloadId);

                                            break;
                                        }

                                        if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                                            downloading = false;
                                        }

                                        final int dl_progress = (int) ((bytes_downloaded * 100l) / bytes_total);


                                        runOnUiThread(new Runnable() {

                                            @Override
                                            public void run() {

                                                mwebView.setVisibility(View.INVISIBLE);
                                                mProgressbar.setProgress((int) dl_progress);
                                                mProgressbar.setVisibility(View.VISIBLE);
                                                mProgressbar.bringToFront();
                                                mprogressTextView.setText(getResources().getString(R.string.downloading)+"\n" + String.valueOf(dl_progress) + "% / 100%");
                                                mprogressTextView.setTextSize(10);
                                                mprogressTextView.setTextColor(Color.BLACK);
                                                mprogressTextView.setVisibility(View.VISIBLE);
                                                mprogressTextView.bringToFront();

                                            }
                                        });

                                        if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {

                                            try {
                                                Thread.sleep(500);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }


                                                returnFile(destinationFile.getAbsolutePath());

                                        }

                                       Log.d(TAG, statusMessage(cursor)); // for debug purposes only
                                        cursor.close();

                                    }

                                }
                            }).start();


                            Log.i(TAG, "destinationfiles: " + Constants.HISTORY_FOLDER + String.valueOf(System.currentTimeMillis() + "." + extension));
/*
                            isURL = false;

                        if (isURL) {
                            view.loadUrl(url);
                        }*/
                    }catch (Exception e)
                        {
                            UI_Utils.callToast(getResources().getString(R.string.oops_try_again), Color.RED, Toast.LENGTH_LONG, getApplicationContext());
                            Log.i(TAG, "shouldOverrideUrlLoading exception: " + (e.getMessage()!=null ? e.getMessage() : e));
                        }

                        }
            }
            return false;
        }
    }


    private String statusMessage(Cursor c) {
        String msg = "???";

        switch (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_FAILED:
                msg = "Download failed!";
                disableProgressBar();
                break;

            case DownloadManager.STATUS_PAUSED:
                msg = "Download paused!";
                disableProgressBar();
                break;

            case DownloadManager.STATUS_PENDING:
                msg = "Download pending!";
                disableProgressBar();
                break;

            case DownloadManager.STATUS_RUNNING:
                msg = "Download in progress!";
                break;

            case DownloadManager.STATUS_SUCCESSFUL:
                msg = "Download complete!";
                disableProgressBar();
                break;

            default:
                msg = "Download is nowhere in sight";
                break;
        }

        return (msg);
    }

    private void disableProgressBar() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mProgressbar.setVisibility(View.INVISIBLE);
                mprogressTextView.setVisibility(View.INVISIBLE);

                mwebView.setVisibility(View.VISIBLE);

            }
        });
    }

    private void openVideoAndImageMediapath(int code) {

        // Create the ACTION_GET_CONTENT Intent
        // Intent intent = FileUtils.createGetContentIntent("image/*, video/*"); // // TODO rony: 31/01/2016 we may need to change it to "*/*" as it will give us filechooser support and more ways to open files. also i think in android 5 or 6 it's not supported the ("image/* , video/*") but not sure
        Intent intent = FileUtils.createGetContentIntent("*/*");
        Intent chooserIntent = Intent.createChooser(intent, "Select Media File");
        startActivityForResult(chooserIntent, code);

    }

    private void openAudioMediapath(int code) {

        // Create the ACTION_GET_CONTENT Intent
        final Intent intent = FileUtils.createGetContentIntent("audio/*");
        Intent chooserIntent = Intent.createChooser(intent, "Select Audio File");
        startActivityForResult(chooserIntent, code);

    }

    private void RecordVideo(int code) {
        // Determine Uri of camera image to save.
        String fname = "MyVideo_"+_destPhoneNumber+".mp4";
        File sdVideoMainDirectory = new File(Constants.TEMP_RECORDING_FOLDER, fname);

        sdVideoMainDirectory.delete();

        _outputFileUri = Uri.fromFile(sdVideoMainDirectory);


        final Intent videoIntent = new Intent(
                MediaStore.ACTION_VIDEO_CAPTURE);
        videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, _outputFileUri);
        videoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30); // set video recording interval
        videoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0); // set the video image quality to low
        videoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 15000);

        startActivityForResult(videoIntent, code); // // TODO rony: 31/01/2016 see native camera opens and not other weird different cameras
    }

    private void takePicture(int code) {

        // Determine Uri of camera image to save.
        String fname = "MyImage_"+_destPhoneNumber+".jpeg";
        File sdImageMainDirectory = new File(Constants.TEMP_RECORDING_FOLDER, fname);
        sdImageMainDirectory.delete();
        _outputFileUri = Uri.fromFile(sdImageMainDirectory);

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
            intent.putExtra(MediaStore.EXTRA_OUTPUT, _outputFileUri);
            cameraIntent = intent;
        }

        startActivityForResult(cameraIntent, code);  // // TODO rony: 31/01/2016 see native camera opens and not other weird different cameras

    }

     private void extractAndReturnFile(Intent intent, SpecialMediaType specialMediaType) {

        FileManager fm;
        Intent resultIntent = new Intent();
        Log.i(TAG, "extractAndReturnFile");
        final boolean isCamera;
        try {
            checkDestinationNumber();
            Uri uri;

            if (intent == null) {
                isCamera = true;
            } else {
                final String action = intent.getAction();
                isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
            }
            if (isCamera) {
                uri = _outputFileUri;
            } else {
                uri = intent.getData();
            }

            // Get the File path from the Uri
            String path = FileUtils.getPath(this, uri);
            // Alternatively, use FileUtils.getFile(Context, Uri)
            if(path == null) {
                throw new FileDoesNotExistException("Path returned from URI was null");
            }

            if (FileUtils.isLocal(path)) {

                if (isCamera) {
                    File file = new File(path);

                  try {
                      String extension = FileManager.extractExtension(path);
                      Log.i(TAG, "isCamera True, Extension saved in camera: " + extension);
                  } catch (FileMissingExtensionException e){

                      Log.w(TAG, "Missing Extension! Adding .jpeg as it is likely to be image file from camera" );
                      file.renameTo(new File(path += ".jpeg"));
                  }
                }

                fm = new FileManager(path);
                Log.i(TAG, "[File selected]: " + path + ". [File Size]: " + FileManager.getFileSizeFormat(fm.getFileSize()));

                resultIntent.putExtra(RESULT_SPECIAL_MEDIA_TYPE, specialMediaType);
                resultIntent.putExtra(RESULT_FILE, fm);
            }

            Log.i(TAG,"End extractAndReturnFile");
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e(TAG, getResources().getString(R.string.file_invalid));
            resultIntent.putExtra(RESULT_ERR_MSG, getResources().getString(R.string.file_invalid));

        } catch (FileExceedsMaxSizeException e) {

            String errMsg = String.format(getResources().getString(R.string.file_over_max_size),
                    FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));
            Log.e(TAG, errMsg);
            resultIntent.putExtra(RESULT_ERR_MSG, errMsg);

        } catch (FileInvalidFormatException | FileDoesNotExistException | FileMissingExtensionException e) {
            e.printStackTrace();
            resultIntent.putExtra(RESULT_ERR_MSG, getResources().getString(R.string.file_invalid));

        } catch (InvalidDestinationNumberException e) {
            resultIntent.putExtra(RESULT_ERR_MSG, getResources().getString(R.string.destnumber_invalid));
        }

        if (getParent() == null) {
            setResult(Activity.RESULT_OK, resultIntent);
        } else {
            getParent().setResult(Activity.RESULT_OK, resultIntent);
        }
        SelectMediaActivity.this.finish();
    }

    private void returnFile(String filePath) {

        SpecialMediaType specialMediaType = null;

        if (SMTypeCode == ActivityRequestCodes.SELECT_CALLER_MEDIA) {
             specialMediaType = SpecialMediaType.CALLER_MEDIA;
        } else if (SMTypeCode == ActivityRequestCodes.SELECT_PROFILE_MEDIA) {
             specialMediaType = SpecialMediaType.PROFILE_MEDIA;
        }

        FileManager fm;
        Intent resultIntent = new Intent();
        Log.i(TAG,"ReturnFile");
        try {

                fm = new FileManager(filePath);
                Log.i(TAG, "[File selected]: " + filePath + ". [File Size]: " + FileManager.getFileSizeFormat(fm.getFileSize()));

                resultIntent.putExtra(RESULT_SPECIAL_MEDIA_TYPE, specialMediaType);
                resultIntent.putExtra(RESULT_FILE, fm);

            Log.i(TAG,"End ReturnFile");
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e(TAG, getResources().getString(R.string.file_invalid));
            resultIntent.putExtra(RESULT_ERR_MSG, getResources().getString(R.string.file_invalid));

        } catch (FileExceedsMaxSizeException e) {

            String errMsg = String.format(getResources().getString(R.string.file_over_max_size),
                    FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));
            Log.e(TAG, errMsg);
            resultIntent.putExtra(RESULT_ERR_MSG, errMsg);

        } catch (FileInvalidFormatException | FileDoesNotExistException | FileMissingExtensionException e) {
            e.printStackTrace();
            resultIntent.putExtra(RESULT_ERR_MSG, getResources().getString(R.string.file_invalid));

        }

        if (getParent() == null) {
            setResult(Activity.RESULT_OK, resultIntent);
        } else {
            getParent().setResult(Activity.RESULT_OK, resultIntent);
        }
        SelectMediaActivity.this.finish();
    }

    private void checkDestinationNumber() throws InvalidDestinationNumberException {

        if(_destPhoneNumber == null)
            throw new InvalidDestinationNumberException();
        if(_destPhoneNumber.equals("") || _destPhoneNumber.length() < 10)
            throw new InvalidDestinationNumberException();
    }

    public void recordAudio() {

        String fname = "MyAudioRecording_"+_destPhoneNumber+".aac";
        File sdAudioFile = new File(Constants.TEMP_RECORDING_FOLDER, fname);

        sdAudioFile.delete();
        _recordedAudioFilePath = sdAudioFile.getAbsolutePath();


        final MediaRecorder recorder = new MediaRecorder();
        ContentValues values = new ContentValues(3);
        values.put(MediaStore.MediaColumns.TITLE, fname);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
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

                returnFile(_recordedAudioFilePath);

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
    //endregion

}
