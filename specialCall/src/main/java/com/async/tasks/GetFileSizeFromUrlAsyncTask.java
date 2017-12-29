package com.async.tasks;

import android.os.AsyncTask;

import com.flows.DownloadFileFlowListener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Mor on 02/09/2016.
 */
public class GetFileSizeFromUrlAsyncTask extends AsyncTask<String, Void, Integer> {

    private DownloadFileFlowListener downloadFileFlowListener;

    public GetFileSizeFromUrlAsyncTask(DownloadFileFlowListener downloadFileFlowListener) {
        this.downloadFileFlowListener = downloadFileFlowListener;
    }

    @Override
    protected Integer doInBackground(String... params) {
        String url = params[0];
        return getFileSizeFromUrl(url);
    }

    @Override
    protected void onPostExecute(Integer fileSize) {
        downloadFileFlowListener.continueDownloadFileFlow(fileSize);
    }

    private int getFileSizeFromUrl(String targetUrl) {
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(targetUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urlConnection != null ? urlConnection.getContentLength() : 0;
    }
}
