package com.handlers.select_media_activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import com.utils.MediaFilesUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.io.File;

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
    protected SelectMediaActivity selectMediaActivity;

    protected void startPreviewActivity(Context ctx, Intent data, boolean isCamera) {

        try {
            String filepath = getFilePathFromIntent(ctx, data, isCamera);

            FileManager managedFile;

            managedFile = new FileManager(filepath);
            if(MediaFilesUtils.canMediaBePrepared(ctx, managedFile)) {
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
