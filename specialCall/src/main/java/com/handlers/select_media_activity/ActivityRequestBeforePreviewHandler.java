package com.handlers.select_media_activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.data.objects.ActivityRequestCodes;
import com.exceptions.FileExceedsMaxSizeException;
import com.files.media.MediaFile;
import com.handlers.Handler;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.mediacallz.app.BuildConfig;
import com.mediacallz.app.R;
import com.ui.activities.PreviewMediaActivity;
import com.ui.activities.SelectMediaActivity;
import com.utils.MediaFileUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;
import com.utils.UtilityFactory;

import java.io.File;
import java.io.IOException;

import static com.utils.MediaFileUtils.MAX_FILE_SIZE;

/**
 * Created by Mor on 16/07/2016.
 */
public abstract class ActivityRequestBeforePreviewHandler implements Handler {

    protected String TAG;
    protected MediaPlayer mMediaPlayer = new MediaPlayer();
    protected SelectMediaActivity selectMediaActivity;
    protected MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);


    protected void startPreviewActivity(Context ctx, Intent data, boolean isCamera) {

        try {
            String filepath = getFilePathFromIntent(ctx, data, isCamera);

            MediaFile managedFile;

            managedFile = new MediaFile(new File(filepath), true);

            if (!canMediaBePrepared(ctx, managedFile)){

                filepath = getFilePathFromIntentForSamsung(ctx, data, isCamera);
                managedFile = new MediaFile(new File(filepath), true);
            }



            if (canMediaBePrepared(ctx, managedFile)) {
                startPreviewActivity(managedFile.getFile().getAbsolutePath());
            } else
                showInvalidFileOrPathToast(selectMediaActivity);

        } catch (FileExceedsMaxSizeException e) {
            e.printStackTrace();
            String errMsg = String.format(ctx.getResources().getString(R.string.file_over_max_size),
                    mediaFileUtils.getFileSizeFormat(MAX_FILE_SIZE));

            UI_Utils.callToast(errMsg, Color.RED, Toast.LENGTH_LONG, ctx);

        } catch (Exception e) {
            e.printStackTrace();
            showInvalidFileOrPathToast(selectMediaActivity);
        }
    }

    protected boolean canMediaBePrepared(Context ctx, MediaFile managedFile) {

        boolean result = true;
        try {
            MediaFile.FileType fType = managedFile.getFileType();
            String filepath = managedFile.getFile().getAbsolutePath();
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

    protected void checkIfWeCanPrepareSound(Context ctx, Uri audioUri) throws IOException {

        Crashlytics.log(Log.INFO, TAG, "Checking if Sound Can Be Prepared and work");
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setDataSource(ctx, audioUri);
        mMediaPlayer.prepare();
        mMediaPlayer.setLooping(true);
    }

    protected void checkIfWeCanPrepareVideo(Context ctx, Uri videoUri) throws Exception {

        Crashlytics.log(Log.INFO, TAG, "Checking if Video Can Be Prepared and work");
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

        Uri uri = getUri(ctx, intent, isCamera);
        resultPath = FileUtils.getPath(ctx, uri);

        refreshMediaScanner(ctx, resultPath);

        if (FileUtils.isLocal(resultPath)) {

            if (isCamera) {
                File file = new File(resultPath);
                String extension = mediaFileUtils.extractExtension(resultPath);
                Crashlytics.log(Log.INFO, TAG, "isCamera True, Extension saved in camera: " + extension);
                if (extension == null) {
                    Crashlytics.log(Log.WARN, TAG, "Missing Extension! Adding .jpeg as it is likely to be image file from camera");
                    file.renameTo(new File(resultPath += ".jpeg"));
                }
            }
        }

        return resultPath;
    }

    protected String getFilePathFromIntentForSamsung(Context ctx, Intent intent, boolean isCamera) throws Exception {

        String resultPath;

        Uri uri = getUri(ctx, intent, isCamera);
        resultPath = FileUtils.getPathForSamsungDevice(ctx, uri);

        refreshMediaScanner(ctx, resultPath);

        if (FileUtils.isLocal(resultPath)) {

            if (isCamera) {
                File file = new File(resultPath);
                String extension = mediaFileUtils.extractExtension(resultPath);
                Crashlytics.log(Log.INFO, TAG, "isCamera True, Extension saved in camera: " + extension);
                if (extension == null) {
                    Crashlytics.log(Log.WARN, TAG, "Missing Extension! Adding .jpeg as it is likely to be image file from camera");
                    file.renameTo(new File(resultPath += ".jpeg"));
                }
            }
        }

        return resultPath;
    }



    private Uri getUri(Context ctx, Intent intent, boolean isCamera) {
        Uri uri;
        if (isCamera) {
            uri = Uri.parse(SharedPrefUtils.getString(ctx, SharedPrefUtils.GENERAL, SharedPrefUtils.SELF_VIDEO_IMAGE_URI));
        } else {
            uri = intent.getData();
        }
        return uri;
    }

    private void refreshMediaScanner(Context ctx, String resultPath) {
        try {
            ctx.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, FileProvider.getUriForFile(ctx, BuildConfig.APPLICATION_ID + ".provider", new File(resultPath))));
        } catch (Exception e) {
            ctx.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(resultPath))));
        }
    }

    private void startPreviewActivity(String filepath) {

        File file = new File(filepath);

        if (!file.exists()) {
            showInvalidFileOrPathToast(selectMediaActivity);
            selectMediaActivity.finish();
            return;
        }

        MediaFile mediaFile = new MediaFile(new File(filepath), true);

        if (mediaFile.getSize() > MAX_FILE_SIZE) {
            String errMsg = String.format(selectMediaActivity.getResources().getString(R.string.file_over_max_size),
                    mediaFileUtils.getFileSizeFormat(MAX_FILE_SIZE));

            UI_Utils.callToast(errMsg, Color.RED, Toast.LENGTH_LONG, selectMediaActivity);
            selectMediaActivity.finish();
            return;
        }

        Intent previewIntentActivity = new Intent(selectMediaActivity, PreviewMediaActivity.class);
        previewIntentActivity.putExtra(PreviewMediaActivity.MANAGED_MEDIA_FILE, mediaFile);
        selectMediaActivity.startActivityForResult(previewIntentActivity, ActivityRequestCodes.PREVIEW_MEDIA);
    }
}
