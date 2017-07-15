package com.flows;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.async.tasks.CompressTask;
import com.async.tasks.MediaProcessingAsyncTask;
import com.async.tasks.RotateTask;
import com.async.tasks.TrimTask;
import com.async.tasks.UploadTask;
import com.data.objects.KeysForBundle;

import java.util.LinkedList;

import com.files.media.MediaFile;
import com.logger.Logger;
import com.logger.LoggerFactory;

/**
 * Created by Mor on 11/08/2016.
 */

/**
 * Responsible for the entire upload file flow, including all media processing (trimming, compressing, rotating)
 */
public class UploadFileFlow implements UploadFileFlowListener {

    private static final String TAG = UploadFileFlow.class.getSimpleName();
    private LinkedList<MediaProcessingAsyncTask> mediaProcTasks;
    private Logger logger = LoggerFactory.getLogger();
    private PostUploadFileFlowLogic postUploadFileFlowLogic;

    public void executeUploadFileFlow(Context context, Bundle bundle, PostUploadFileFlowLogic postUploadFileFlowLogic) {
        logger.info(TAG, "Starting upload file flow...");
        
        this.postUploadFileFlowLogic = postUploadFileFlowLogic;
        mediaProcTasks = new LinkedList<>();
        mediaProcTasks.add(new TrimTask(0, this, context));
        mediaProcTasks.add(new CompressTask(1, this, context));
        mediaProcTasks.add(new RotateTask(2, this, context));

        continueUploadFileFlow(context, 0, bundle);
    }

    @Override
    public void continueUploadFileFlow(Context context, int order, Bundle bundle) {
        if (order == mediaProcTasks.size()) {
            uploadFile(context, bundle, postUploadFileFlowLogic);
            mediaProcTasks = null;
            return;
        }

        MediaProcessingAsyncTask task = mediaProcTasks.get(order);
        logger.info(TAG, "Handling media processing task: " + task.getClass().getSimpleName());
        order++;

        MediaFile fileForUpload = (MediaFile) bundle.get(KeysForBundle.FILE_FOR_UPLOAD);
        if (task.isProcessingNeeded(context, fileForUpload)) {
            logger.info(TAG, "Processing task: " + task.getClass().getSimpleName() + " is needed. Executing...");
            task.execute(bundle);
        } else {
            continueUploadFileFlow(context, order, bundle);
        }
    }

    private void uploadFile(Context context, Bundle bundle, PostUploadFileFlowLogic postUploadFileFlowLogic) {
        if (bundle != null) {
            UploadTask uploadTask = new UploadTask(context, bundle, postUploadFileFlowLogic);
            uploadTask.execute();
        }
    }
}
