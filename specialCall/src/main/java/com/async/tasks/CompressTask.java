package com.async.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.data.objects.KeysForBundle;
import com.flows.UploadFileFlowListener;
import com.mediacallz.app.R;
import com.netcompss.loader.LoadJNI;
import com.utils.MediaFileProcessingUtils;

import java.io.File;

import com.files.media.MediaFile;
import com.utils.MediaFilesUtils;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 10/08/2016.
 */
public class CompressTask extends MediaProcessingAsyncTask {

    public CompressTask(int order, UploadFileFlowListener uploadFileFlow, Context context) {
        super(order, uploadFileFlow, context);
    }

    @Override
    protected void onPreExecute() {

        String cancel = context.getResources().getString(R.string.cancel);

        progressDialog = new ProgressDialog(context);
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(context.getResources().getString(R.string.compressing_file));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
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
        try {
            bundle = params[0];
            baseFile = (MediaFile) bundle.get(KeysForBundle.FILE_FOR_UPLOAD);
            processedFilePath = OUT_FOLDER + getProcessedFileName(baseFile, "comp");

            workerThread = new Thread(new Runnable() {

                @Override
                public void run() {

                    Log.d(TAG, "Worker started");
                    vk = new LoadJNI();

                    processedFile = mediaFileProcessingUtils.compressMediaFile(baseFile, processedFilePath, context);
                    handler.sendEmptyMessage(MediaFileProcessingUtils.FINISHED_TRANSCODING_MSG);
                }
            });

            startUpdateProgressThread();

            workerThread.start();
            workerThread.join();

            bundle.putSerializable(KeysForBundle.FILE_FOR_UPLOAD, processedFile);

        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Failed to compress file. Exception:" + e.getMessage());
            bundle.putSerializable(KeysForBundle.FILE_FOR_UPLOAD, baseFile);
        }
        return bundle;
    }

    @Override
    protected void onCancelled() {
        progressDialog.dismiss();
        workerThread.interrupt();
        updateProgressThread.interrupt();
        MediaFilesUtils.delete(new File(processedFilePath));
        sendLoadingCancelled(context, TAG);
    }

    @Override
    public boolean isProcessingNeeded(Context ctx, MediaFile baseFile) {
        return mediaFileProcessingUtils.isCompressionNeeded(context, baseFile);
    }

    @Override
    public void onPostExecute(Bundle bundle) {
        if (!isCancelled())
            uploadFileFlow.continueUploadFileFlow(context, order + 1, bundle);
    }

}
