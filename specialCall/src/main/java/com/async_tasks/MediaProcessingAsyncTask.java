package com.async_tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import com.utils.BroadcastUtils;

import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;

/**
 * Created by Mor on 20/07/2016.
 */
public abstract class MediaProcessingAsyncTask extends AsyncTask<Bundle, Integer, Bundle> {

    protected int order;

    public MediaProcessingAsyncTask(int order) {
        this.order=order;
    }

    @Override
    protected Bundle doInBackground(Bundle... params) {
        return null;
    }

    public abstract boolean isProcessingNeeded(Context ctx, FileManager baseFile);

    protected void sendLoadingCancelled(Context ctx, String tag) {
        BroadcastUtils.sendEventReportBroadcast(ctx, tag, new EventReport(EventType.LOADING_CANCEL));
    }
}
