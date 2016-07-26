package com.handlers.select_media_activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.data_objects.ActivityRequestCodes;
import com.handlers.Handler;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.mediacallz.app.R;
import com.ui.activities.PreviewMediaActivity;
import com.ui.activities.SelectMediaActivity;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.io.File;
import java.io.IOException;

import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;

/**
 * Created by Mor on 16/07/2016.
 */
public abstract class ActivityRequestBeforePreviewHandler implements Handler {

    protected String TAG;
    protected MediaPlayer mMediaPlayer = new MediaPlayer();
    protected SelectMediaActivity selectMediaActivity;

    protected void startPreviewActivity(Context ctx, Intent data, boolean isCamera) {

        try {
            String filepath = getFilePathFromIntent(ctx, data, isCamera);

            FileManager managedFile;

            managedFile = new FileManager(filepath);
            if(canMediaBePrepared(ctx, managedFile)) {
                startPreviewActivity(managedFile.getFileFullPath());
            }
            else
                showInvalidFileOrPathToast(selectMediaActivity);

        } catch(FileExceedsMaxSizeException e) {
            e.printStackTrace();
            String errMsg = String.format(ctx.getResources().getString(R.string.file_over_max_size),
                    FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));

            UI_Utils.callToast(errMsg, Color.RED, Toast.LENGTH_LONG, ctx);

        } catch (Exception e) {
            e.printStackTrace();
            showInvalidFileOrPathToast(selectMediaActivity);
        }
    }

    protected boolean canMediaBePrepared(Context ctx, FileManager managedFile) {

        boolean result = true;
        try {
            FileManager.FileType fType = managedFile.getFileType();
            String filepath = managedFile.getFileFullPath();
            final File root = new File(filepath);
            Uri uri = Uri.fromFile(root);

            switch (fType) {
                case AUDIO:
                    checkIfWeCanPrepareSound(ctx, uri);
                    break;

                case VIDEO:
                    checkIfWeCanPrepareVideo(ctx, uri);
                    break;

                case IMAGE:
                    break;
            }
        } catch (Exception e) {
            result = false;
        }
        return result;

    }

    protected void checkIfWeCanPrepareSound(Context ctx ,Uri audioUri) throws IOException {

        Crashlytics.log(Log.INFO,TAG, "Checking if Sound Can Be Prepared and work");
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setDataSource(ctx, audioUri);
        mMediaPlayer.prepare();
        mMediaPlayer.setLooping(true);
    }

    protected void checkIfWeCanPrepareVideo(Context ctx, Uri videoUri) throws Exception {

        Crashlytics.log(Log.INFO,TAG, "Checking if Video Can Be Prepared and work");
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setDataSource(ctx, videoUri);
        mMediaPlayer.prepare();
        mMediaPlayer.setLooping(true);
        int width = mMediaPlayer.getVideoWidth();
        int height = mMediaPlayer.getVideoHeight();
        if (width <= 0 || height <= 0) {
            throw new Exception();
        }
    }

    protected void showInvalidFileOrPathToast(Context ctx) {
        UI_Utils.callToast(ctx.getResources().getString(R.string.file_invalid),
                Color.RED, Toast.LENGTH_LONG, ctx);
    }

    protected String getFilePathFromIntent(Context ctx, Intent intent, boolean isCamera) throws Exception {

        String resultPath;
        Uri uri;


        if (isCamera) {
            uri = Uri.parse(SharedPrefUtils.getString(ctx ,SharedPrefUtils.GENERAL,SharedPrefUtils.SELF_VIDEO_IMAGE_URI));
        } else {
            uri = intent.getData();
        }

        // Get the File path from the Uri
        resultPath = FileUtils.getPath(ctx, uri);
        // Alternatively, use FileUtils.getFile(Context, Uri)
        if (resultPath == null) {
            resultPath = uri.getLastPathSegment();
            if (resultPath == null)
                throw new FileDoesNotExistException("Path returned from URI was null");
        }

        ctx.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(resultPath))));

        if (FileUtils.isLocal(resultPath)) {

            if (isCamera) {
                File file = new File(resultPath);

                try {
                    String extension = FileManager.extractExtension(resultPath);
                    Crashlytics.log(Log.INFO, TAG, "isCamera True, Extension saved in camera: " + extension);
                } catch (FileMissingExtensionException e) {

                    Crashlytics.log(Log.WARN, TAG, "Missing Extension! Adding .jpeg as it is likely to be image file from camera");
                    file.renameTo(new File(resultPath += ".jpeg"));
                }
            }
        }

        return resultPath;
    }

    private void startPreviewActivity(String filepath) {

        FileManager managedFile;
        try {
            managedFile = new FileManager(filepath);

            Intent previewIntentActivity = new Intent(selectMediaActivity, PreviewMediaActivity.class);
            previewIntentActivity.putExtra(PreviewMediaActivity.MANAGED_MEDIA_FILE, managedFile);
            previewIntentActivity.putExtra(SelectMediaActivity.SPECIAL_MEDIA_TYPE, selectMediaActivity.getSMTypeCode());
            selectMediaActivity.startActivityForResult(previewIntentActivity, ActivityRequestCodes.PREVIEW_MEDIA);

        } catch(FileExceedsMaxSizeException e) {
            e.printStackTrace();
            String errMsg = String.format(selectMediaActivity.getResources().getString(R.string.file_over_max_size),
                    FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));

            UI_Utils.callToast(errMsg, Color.RED, Toast.LENGTH_LONG, selectMediaActivity);
            selectMediaActivity.finish();

        } catch (FileMissingExtensionException | FileDoesNotExistException | FileInvalidFormatException e) {
            e.printStackTrace();
            showInvalidFileOrPathToast(selectMediaActivity);
            selectMediaActivity.finish();
        }
    }


    @Override
    public void handle(Context ctx, Object... params) {

    }
}
