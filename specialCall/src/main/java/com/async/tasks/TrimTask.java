package com.async.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.data.objects.KeysForBundle;
import com.flows.UploadFileFlowListener;
import com.mediacallz.app.R;
import com.utils.MediaFileProcessingUtils;
import com.utils.SharedPrefUtils;

import java.io.File;

import com.files.media.MediaFile;

/**
 * Created by Mor on 10/08/2016.
 */
public class TrimTask extends MediaProcessingAsyncTask {

    public TrimTask(int order, UploadFileFlowListener uploadFileFlow, Context context) {
       super(order, uploadFileFlow, context);
    }

    @Override
    protected void onPreExecute() {

        calcProgress = true;

        String cancel = context.getResources().getString(R.string.cancel);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT)
            progressDialog = new ProgressDialog(context,R.style.AlertDialogCustom);
        else
            progressDialog = new ProgressDialog(context);
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(context.getResources().getString(R.string.trimming));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setProgress(0);
        progressDialog.setMax(100);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                handler.sendEmptyMessage(MediaFileProcessingUtils.STOP_TRANSCODING_MSG);
                instance.cancel(true);
            }
        });

        progressDialog.show();
    }

    @Override
    protected Bundle doInBackground(Bundle... params) {

        Log.d(TAG, "Worker started");
        bundle = params[0];
        baseFile = (MediaFile) bundle.get(KeysForBundle.FILE_FOR_UPLOAD);

        if (baseFile.getFileType() == MediaFile.FileType.AUDIO)
        {
            processedFilePath = AUDIO_OUT_FOLDER + getProcessedFileName(baseFile, (SharedPrefUtils.getInt(context, SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_START_TRIM_IN_MILISEC)/1000)+"_trimmed");
            SharedPrefUtils.setBoolean(context,SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_HISTORY_EXIST,true);
        }
        else
             processedFilePath = OUT_FOLDER + getProcessedFileName(baseFile, (SharedPrefUtils.getInt(context, SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_START_TRIM_IN_MILISEC)/1000)+"_trimmed");

        workerThread = new Thread(new Runnable() {

            @Override
            public void run() {

                processedFile = mediaFileProcessingUtils.trimMediaFile(baseFile, processedFilePath, context);

                try {
                    Thread.sleep(1000); // Sleeping so in fast trimmings the dialog won't appear and disappear too fast (like a blink)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                handler.sendEmptyMessage(MediaFileProcessingUtils.FINISHED_TRANSCODING_MSG);

            }
        });

        startUpdateProgressThread();

        workerThread.start();


        try {
            workerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        bundle.putSerializable(KeysForBundle.FILE_FOR_UPLOAD, processedFile);
        return bundle;
    }

    @Override
    protected void onCancelled() {
        progressDialog.dismiss();
        workerThread.interrupt();
        updateProgressThread.interrupt();
        mediaFileUtils.delete(new File(processedFilePath));
        sendLoadingCancelled(context, TAG);
    }

    @Override
    public boolean isProcessingNeeded(Context ctx, MediaFile baseFile) {
        return mediaFileProcessingUtils.isTrimNeeded(ctx, baseFile);
    }

    @Override
    public void onPostExecute(Bundle bundle) {
        if (!isCancelled())
            uploadFileFlow.continueUploadFileFlow(context, order + 1, bundle);
    }

}
