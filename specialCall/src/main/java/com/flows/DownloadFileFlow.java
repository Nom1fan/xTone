package com.flows;

import android.content.Context;

import com.async.tasks.DownloadFileAsyncTask;
import com.async.tasks.DownloadFileAsyncTask.PostDownloadCallBackListener;
import com.async.tasks.GetFileSizeFromUrlAsyncTask;

import java.io.IOException;

/**
 * Created by Mor on 02/09/2016.
 */
public class DownloadFileFlow implements DownloadFileFlowListener {

    private Context context;
    private String url;
    private PostDownloadCallBackListener listener;

    public void startDownloadFileFlow(Context context, String url) {
        this.context = context;
        this.url = url;
        new GetFileSizeFromUrlAsyncTask(this).execute(url);
    }

    @Override
    public void continueDownloadFileFlow(int fileSize) {
        try {
            DownloadFileAsyncTask task = new DownloadFileAsyncTask(context, url, fileSize);
            task.setListener(listener);
            task.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setListener(PostDownloadCallBackListener listener) {
        this.listener = listener;
    }
}
