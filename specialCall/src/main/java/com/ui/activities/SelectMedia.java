package com.ui.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.data_objects.Constants;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.services.StorageServerProxyService;
import com.special.app.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import DataObjects.SharedConstants;
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
public class SelectMedia extends Activity implements View.OnClickListener{

    private static final String TAG = SelectMedia.class.getSimpleName();
    private Uri _outputFileUri;
    private abstract class ActivityRequestCodes {

        public static final int SELECT_CALLER_MEDIA = 1;

        public static final int SELECT_PROFILE_MEDIA = 3;

    }
    private int SMTypeCode;
    private String _destPhoneNumber = "";
    private String _destName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        MainActivity.wasFileChooser=true;
        Intent intent = getIntent();
        setContentView(R.layout.select_media);

        Button button1 = (Button) findViewById(R.id.back);
        button1.setOnClickListener(this);
        TextView mediaType = (TextView) findViewById(R.id.selectMediaType);

        _destPhoneNumber = intent.getStringExtra("DestinationNumber");
        _destName = intent.getStringExtra("DestinationName");
        SMTypeCode = intent.getIntExtra("SpecialMediaType", 1);

        if (ActivityRequestCodes.SELECT_CALLER_MEDIA == SMTypeCode)
           mediaType.setText(R.string.select_caller_media_title);
        else
            mediaType.setText(R.string.select_profile_media_title);

        TextView nameOrPhone = (TextView) findViewById(R.id.contactNameOrNumber);

      if (_destName.isEmpty())
        nameOrPhone.setText(_destPhoneNumber);
        else
          nameOrPhone.setText(_destName);

        Button videoBtn = (Button) findViewById(R.id.video_or_image);
        videoBtn.setOnClickListener(this);

        Button recordVideoBtn = (Button) findViewById(R.id.recordVideo);
        recordVideoBtn.setOnClickListener(this);

        Button takePictureBtn = (Button) findViewById(R.id.takePicture);
        takePictureBtn.setOnClickListener(this);

        Button audioBtn = (Button) findViewById(R.id.audio);
        audioBtn.setOnClickListener(this);

        Button recordAudioBtn = (Button) findViewById(R.id.recordAudio);
        recordAudioBtn.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        int id = v.getId();

        if (id == R.id.back) {
            SelectMedia.this.finish();
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

            RecordAudio(SMTypeCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i(TAG, "onActivityResult");
        if (resultCode == RESULT_OK) {
            Log.i(TAG, "requestCode: " + requestCode);
            if (requestCode == ActivityRequestCodes.SELECT_CALLER_MEDIA) {

                SpecialMediaType specialMediaType = SpecialMediaType.CALLER_MEDIA;
                uploadFile(data, specialMediaType);
            } else if (requestCode == ActivityRequestCodes.SELECT_PROFILE_MEDIA) {

                SpecialMediaType specialMediaType = SpecialMediaType.PROFILE_MEDIA;
                uploadFile(data, specialMediaType);
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

    	/* -------------- Assisting methods -------------- */

    private void openVideoAndImageMediapath(int code) {

        // Create the ACTION_GET_CONTENT Intent
        Intent intent = FileUtils.createGetContentIntent("image/*, video/*"); // // TODO rony: 31/01/2016 we may need to change it to "*/*" as it will give us filechooser support and more ways to open files. also i think in android 5 or 6 it's not supported the ("image/* , video/*") but not sure
        Intent chooserIntent = Intent.createChooser(intent, "Select Media File");
        startActivityForResult(chooserIntent, code);

    }

    private void openAudioMediapath(int code) {

        // Create the ACTION_GET_CONTENT Intent
        final Intent intent = FileUtils.createGetContentIntent("audio/*");
        Intent chooserIntent = Intent.createChooser(intent, "Select Media File");
        startActivityForResult(chooserIntent, code);

    }

    private void RecordVideo(int code) {
        // Determine Uri of camera image to save.
        String fname = "MyVideo.mp4";
        File sdImageMainDirectory = new File(Constants.TEMP_RECORDING_FOLDER, fname);
        _outputFileUri = Uri.fromFile(sdImageMainDirectory);


        final Intent videoIntent = new Intent(
                MediaStore.ACTION_VIDEO_CAPTURE);
        videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, _outputFileUri);
        startActivityForResult(videoIntent, code); // // TODO rony: 31/01/2016 see native camera opens and not other weird different cameras
    }

    private void takePicture(int code) {

        // Determine Uri of camera image to save.
        String fname = "MyImage";
        File sdImageMainDirectory = new File(Constants.TEMP_RECORDING_FOLDER, fname);
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

    private void RecordAudio(int code) {


        String fname = "MyAudioRecording.3gp";
        File sdImageMainDirectory = new File(Constants.TEMP_RECORDING_FOLDER, fname);
        _outputFileUri = Uri.fromFile(sdImageMainDirectory);

        recordAudio(sdImageMainDirectory.getAbsolutePath());

    }

    private void uploadFile(Intent data, SpecialMediaType specialMediaType) {

            FileManager fm = null;
            Log.i(TAG,"Upload File");
            final boolean isCamera;
            try {
                checkDestinationNumber();
                Uri uri;

                if (data == null) {
                    isCamera = true;
                } else {
                    final String action = data.getAction();
                    isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
                }
                if (isCamera) {
                    uri = _outputFileUri;
                } else {
                    uri = data.getData();
                }

                // Get the File path from the Uri
                String path = FileUtils.getPath(this, uri);
                // Alternatively, use FileUtils.getFile(Context, Uri)
                if (path != null) if (FileUtils.isLocal(path)) {

                    if (isCamera) {
                        File file = new File(path);
                        file.renameTo(new File(path += ".jpeg"));
                    }

                    fm = new FileManager(path);
                    MainActivity.wasFileChooser = true;
                    Log.i(TAG, "onActivityResult RESULT_OK _ Rony");
                    Intent i = new Intent(getApplicationContext(), StorageServerProxyService.class);
                    i.setAction(StorageServerProxyService.ACTION_UPLOAD);
                    i.putExtra(StorageServerProxyService.DESTINATION_ID, _destPhoneNumber);
                    i.putExtra(StorageServerProxyService.SPECIAL_MEDIA_TYPE, specialMediaType);
                    i.putExtra(StorageServerProxyService.FILE_TO_UPLOAD, fm);
                    getApplicationContext().startService(i);

                }
                Log.i(TAG,"End Of Upload File");
            } catch (NullPointerException e) {
                e.printStackTrace();
                Log.e(TAG, "It seems there was a problem with the file path.");
                data.putExtra("msg", "Oops! problem with file");
            } catch (FileExceedsMaxSizeException e) {
                data.putExtra("msg", "Oops! Select a file that weights less than:" +
                        FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));
            } catch (FileInvalidFormatException | FileDoesNotExistException | FileMissingExtensionException e) {
                e.printStackTrace();
                data.putExtra("msg", "Oops! Invalid file");
            } catch (InvalidDestinationNumberException e) {
                data.putExtra("msg", "Oops! Invalid destination number");
            }

        if (getParent() == null) {
            setResult(Activity.RESULT_OK, data);
        } else {
            getParent().setResult(Activity.RESULT_OK, data);
        }
            SelectMedia.this.finish();
        }

    private void uploadAudioFile(Uri uri) {
        Intent data = new Intent();
        FileManager fm = null;
        Log.i(TAG,"Upload Audio File");
        final boolean isCamera;
        try {
            checkDestinationNumber();

            // Get the File path from the Uri
            String path = FileUtils.getPath(this, uri);

            // Alternatively, use FileUtils.getFile(Context, Uri)
            if (path != null) if (FileUtils.isLocal(path)) {

                fm = new FileManager(path);
                MainActivity.wasFileChooser = true;
                Intent i = new Intent(getApplicationContext(), StorageServerProxyService.class);
                i.setAction(StorageServerProxyService.ACTION_UPLOAD);
                i.putExtra(StorageServerProxyService.DESTINATION_ID, _destPhoneNumber);


                if (ActivityRequestCodes.SELECT_CALLER_MEDIA == SMTypeCode)
                    i.putExtra(StorageServerProxyService.SPECIAL_MEDIA_TYPE, SpecialMediaType.CALLER_MEDIA);
                else
                    i.putExtra(StorageServerProxyService.SPECIAL_MEDIA_TYPE, SpecialMediaType.PROFILE_MEDIA);


                i.putExtra(StorageServerProxyService.FILE_TO_UPLOAD, fm);
                getApplicationContext().startService(i);

            }
            Log.i(TAG,"End Of Upload Audio File");
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e(TAG, "It seems there was a problem with the file path.");
            data.putExtra("msg", "Oops! problem with file");
        } catch (FileExceedsMaxSizeException e) {
            data.putExtra("msg", "Oops! Select a file that weights less than:" +
                    FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));
        } catch (FileInvalidFormatException | FileDoesNotExistException | FileMissingExtensionException e) {
            e.printStackTrace();
            data.putExtra("msg", "Oops! Invalid file");
        } catch (InvalidDestinationNumberException e) {
            data.putExtra("msg", "Oops! Invalid destination number");
        }

        if (getParent() == null) {
            setResult(Activity.RESULT_OK, data);
        } else {
            getParent().setResult(Activity.RESULT_OK, data);
        }

        SelectMedia.this.finish();

    }

    private void checkDestinationNumber() throws InvalidDestinationNumberException {

        if(_destPhoneNumber ==null)
            throw new InvalidDestinationNumberException();
        if(_destPhoneNumber.equals("") || _destPhoneNumber.length() < 10)
            throw new InvalidDestinationNumberException();
    }

    public void recordAudio(String fileName) {

        final MediaRecorder recorder = new MediaRecorder();
        ContentValues values = new ContentValues(3);
        values.put(MediaStore.MediaColumns.TITLE, fileName);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setOutputFile(fileName);


        try {
            recorder.prepare();
        } catch (Exception e){
            e.printStackTrace();
        }

        final ProgressDialog mProgressDialog = new ProgressDialog(SelectMedia.this);  // // TODO rony: 31/01/2016  add seconds to the progress of the recording
        mProgressDialog.setTitle("Recording...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setButton("Stop recording", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mProgressDialog.dismiss();
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();

                    uploadAudioFile(_outputFileUri); // Upload Recorded Audio File
                }
            }
        });

        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface p1) {
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                }
            }
        });
        recorder.start();
        mProgressDialog.show();
    }
}
