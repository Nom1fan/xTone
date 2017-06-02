package com.async.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.data.objects.KeysForBundle;
import com.flows.UploadFileFlowListener;
import com.mediacallz.app.R;

import java.io.File;

import com.files.media.MediaFile;
import com.utils.MediaFilesUtilsImpl;

/**
 * Created by Mor on 11/08/2016.
 */
public class RotateTask extends MediaProcessingAsyncTask {

    public RotateTask(int order, UploadFileFlowListener uploadFileFlow, Context context) {
        super(order, uploadFileFlow, context);
    }

    @Override
    protected void onPreExecute() {

        String cancel = context.getResources().getString(R.string.cancel);

        progressDialog = new ProgressDialog(context);
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(context.getResources().getString(R.string.processing_image));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
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
        processedFilePath = OUT_FOLDER + getProcessedFileName(baseFile, "rotated");

        processedFile = mediaFileProcessingUtils.rotateImageFile(baseFile, processedFilePath, context);
        bundle.putSerializable(KeysForBundle.FILE_FOR_UPLOAD, processedFile);

        return bundle;
    }

    @Override
    protected void onCancelled() {
        progressDialog.dismiss();
        MediaFilesUtilsImpl.delete(new File(processedFilePath));
        sendLoadingCancelled(context, TAG);
    }

    @Override
    public boolean isProcessingNeeded(Context ctx, MediaFile baseFile) {
        return mediaFileProcessingUtils.isRotationNeeded(ctx, baseFile.getFileType());
    }

    @Override
    public void onPostExecute(Bundle bundle) {
        progressDialog.dismiss();
        if (!isCancelled())
            uploadFileFlow.continueUploadFileFlow(context, order + 1, bundle);
    }

}
