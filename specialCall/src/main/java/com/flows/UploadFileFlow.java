package com.flows;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.async_tasks.CompressTask;
import com.async_tasks.MediaProcessingAsyncTask;
import com.async_tasks.RotateTask;
import com.async_tasks.TrimTask;
import com.async_tasks.UploadTask;
import com.data_objects.KeysForBundle;

import java.util.LinkedList;

import FilesManager.FileManager;

/**
 * Created by Mor on 11/08/2016.
 */

/**
 * Responsible for the entire upload file flow, including all media processing (trimming, compressing, rotating)
 */
public class UploadFileFlow implements UploadFileFlowListener {

    private static final String TAG = UploadFileFlow.class.getSimpleName();
    private LinkedList<MediaProcessingAsyncTask> mediaProcTasks;

    public void executeUploadFileFlow(Context context, Bundle bundle) {
        Log.i(TAG, "Starting upload file flow...");
        mediaProcTasks = new LinkedList<>();
        mediaProcTasks.add(new TrimTask(0, this, context));
        mediaProcTasks.add(new CompressTask(1, this, context));
        mediaProcTasks.add(new RotateTask(2, this, context));

        continueUploadFileFlow(context, 0, bundle);
    }

    @Override
    public void continueUploadFileFlow(Context context, int order, Bundle bundle) {
        if (order == mediaProcTasks.size()) {
            uploadFile(context, bundle);
            mediaProcTasks = null;
            return;
        }

        MediaProcessingAsyncTask task = mediaProcTasks.get(order);
        Log.i(TAG, "Handling media processing task: " + task.getClass().getSimpleName());
        order++;

        FileManager fileForUpload = (FileManager) bundle.get(KeysForBundle.FILE_FOR_UPLOAD);
        if (task.isProcessingNeeded(context, fileForUpload)) {
            Log.i(TAG, "Processing task: " + task.getClass().getSimpleName() + " is needed. Executing...");
            task.execute(bundle);
        } else {
            continueUploadFileFlow(context, order, bundle);
        }
    }

    private void uploadFile(Context context, Bundle bundle) {
        if (bundle != null) {
            UploadTask uploadTask = new UploadTask(context, bundle);
            uploadTask.execute();
        }
    }
}
