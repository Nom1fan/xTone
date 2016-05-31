package com.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.data_objects.ActivityRequestCodes;
import com.mediacallz.app.R;
import com.services.AbstractStandOutService;
import com.services.PreviewService;
import com.utils.BitmapUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.io.File;

import DataObjects.SpecialMediaType;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;

/**
 * Created by rony on 29/01/2016.
 */
public class PreviewMediaActivity extends Activity implements View.OnClickListener {

    public static final String MEDIA_FILE_PATH = "FilePathForMediaFile";
    public static final String RESULT_ERR_MSG = "ResultErrMsg";
    public static final String RESULT_SPECIAL_MEDIA_TYPE = "ResultSpecialMediaType";
    public static final String RESULT_FILE = "ResultFile";

    private static final String TAG = PreviewMediaActivity.class.getSimpleName();
    private String _filePath = "";
    private float oldPosition =0;
    private int moveLength= 0;
    private int SMTypeCode;
    private boolean _isPreview = false;
    private Button previewFile;
    private ImageButton imageButton;
    private FileManager.FileType fType;
    //region Activity methods (onCreate(), onPause()...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        Intent intent = getIntent();
        _filePath = intent.getStringExtra(MEDIA_FILE_PATH);
        SMTypeCode = intent.getIntExtra(SelectMediaActivity.SPECIAL_MEDIA_TYPE, 1);
        initializePreviewMediaUI();

    }

    private void initializePreviewMediaUI() {

        closePreview();

        try {
            new FileManager(_filePath);
        }catch (FileExceedsMaxSizeException e) {

            e.printStackTrace();
            //TODO change errMsg to be returned to MainAcivity to appear in snackBar instead ot toast
            String errMsg = String.format(getResources().getString(R.string.file_over_max_size),
                    FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));

            UI_Utils.callToast(errMsg, Color.RED, Toast.LENGTH_LONG, getApplicationContext());
            finish();
            return;
        } catch (FileMissingExtensionException | FileDoesNotExistException | FileInvalidFormatException e) {
            e.printStackTrace();
            UI_Utils.callToast(getResources().getString(R.string.file_invalid), Color.RED, Toast.LENGTH_LONG, getApplicationContext());
            finish();
            return;
        }

        try {
            fType = FileManager.getFileType(_filePath);
        }
        catch(FileMissingExtensionException e)
        {
            Log.i(TAG , "FileMissingExtensionException in startPreviewStandoutWindow with " + _filePath);
            e.printStackTrace();

            UI_Utils.callToast(" error with file , missing extension ", Color.RED, Toast.LENGTH_SHORT, getApplicationContext());
            return;
        } catch(FileInvalidFormatException e)
        {
            Log.i(TAG , "FileInvalidFormatException in startPreviewStandoutWindow with " + _filePath);
            e.printStackTrace();

            UI_Utils.callToast(" Invalid format file please choose other file ", Color.RED, Toast.LENGTH_SHORT, getApplicationContext());
            return;
        } catch (FileDoesNotExistException e) {
            Log.i(TAG , "FileDoesNotExistException in startPreviewStandoutWindow with " + _filePath);
            e.printStackTrace();

            UI_Utils.callToast(" Invalid format file please choose other file ", Color.RED, Toast.LENGTH_SHORT, getApplicationContext());
            return;
        }

        setContentView(R.layout.preview_before_upload);

        TextView fileType = (TextView) findViewById(R.id.upload_file_type);
        TextView fileName = (TextView) findViewById(R.id.upload_file_name);
        fileName.setText(FileManager.getFileNameWithExtension(_filePath));

        Button upload = (Button) findViewById(R.id.upload_btn);
        upload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closePreview();
                returnFile(_filePath);
            }
        });

        Button cancel = (Button) findViewById(R.id.cancel_btn);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closePreview();
                finish();
            }
        });

        previewFile = (Button) findViewById(R.id.playPreview);
        previewFile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (_isPreview) {
                    closePreview();
                    _isPreview = false;
                    previewFile.setText(getResources().getString(R.string.play_preview));
                } else {
                    startPreviewStandoutWindow(_filePath , fType);
                    _isPreview = true;
                    previewFile.setText(getResources().getString(R.string.stop_preview));
                }
            }
        });



        switch (fType) {
            case AUDIO:
                fileType.setText(getResources().getString(R.string.fileType_audio));
                break;

            case VIDEO:
                fileType.setText(getResources().getString(R.string.fileType_video));
                break;

            case IMAGE:
                fileType.setText(getResources().getString(R.string.fileType_image));
                break;
        }


    }


    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);

        imageButton = (ImageButton) findViewById(R.id.preview_thumbnail);
        Log.i(TAG, "type and path " + fType + "  " + _filePath);


        switch (fType) {
            case AUDIO:
                imageButton.setImageResource(R.drawable.ringtone_icon);
                break;

            case VIDEO:
            case IMAGE:

                BitmapUtils.execBitMapWorkerTask(imageButton, fType, _filePath, false);
                // stretch the uploaded image as it won't stretch because we use a drawable instead that we don't want to stretch
                imageButton.setPadding(0, 0, 0, 0);
                imageButton.setScaleType(ImageView.ScaleType.FIT_XY);

                break;
        }



        imageButton.setVisibility(View.VISIBLE);

    }


    @Override
    protected void onPause() {
        super.onPause();
        closePreview();
        overridePendingTransition(R.anim.no_animation_no_delay, R.anim.slide_out_up);// close drawer animation
        Log.i(TAG, "onPause()");
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
                        PreviewMediaActivity.this.finish();
                    }

                return true;

            default :
                return super.onTouchEvent(event);
        }
    }
    //endregion

    //region Assisting methods (onClick(), takePicture(), ...)

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

            getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(filePath))));

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

        Log.i(TAG, "starting MainActivity with Clear Top");

        PreviewMediaActivity.this.finish();

       // resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      //  startActivityForResult(resultIntent,ActivityRequestCodes.SELECT_MEDIA);

    }

    @Override
    public void onClick(View v) {

    }

    private void startPreviewStandoutWindow(String filepath , FileManager.FileType fType) {

        AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME, am.getStreamVolume(AudioManager.STREAM_MUSIC));
        Log.i(TAG, "PreviewStart MUSIC_VOLUME Original" + String.valueOf(am.getStreamVolume(AudioManager.STREAM_MUSIC)));

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

    //endregion


}
