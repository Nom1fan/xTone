package com.async_tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;

import com.mediacallz.app.R;
import com.utils.MediaFilesUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Mor on 23/08/2016.
 */
public class DownloadFileAsyncTask extends AsyncTask<Void, Integer, File> {

    private static final String TAG = DownloadFileAsyncTask.class.getSimpleName();

    private static final File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    private Context context;
    private DownloadFileAsyncTask taskInstance;
    private int fileSize;
    private String url;
    private InputStream in;
    private PowerManager.WakeLock wakeLock;
    private PostDownloadCallBackListener listener;

    public DownloadFileAsyncTask(Context context, String targetUrl, int fileSize) throws IOException {
        this.context=context;
        this.taskInstance = this;
        this.url = targetUrl;
        this.fileSize = fileSize;
    }

    private ProgressDialog progressDialog;
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        String cancel = context.getResources().getString(R.string.cancel);

        progressDialog = new ProgressDialog(context);
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(context.getResources().getString(R.string.downloading));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.setMax(fileSize);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                taskInstance.cancel(true);
            }
        });

        progressDialog.show();
    }

    @Override
    protected File doInBackground(Void... voids) {
        String filename = MediaFilesUtils.getFileNameByUrl(url);
        return downloadFile(filename);
    }

    @Override
    protected void onPostExecute(File downloadedFile) {
        progressDialog.dismiss();
        if(listener!=null)
            listener.doCallBack(downloadedFile);
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {

        if (progressDialog != null) {
            progressDialog.incrementProgressBy(progress[0]);
        }
    }

    public File downloadFile(String fileName) {

        FileOutputStream fos = null;
        File downloadedFile = null;
        try {

            PowerManager powerManager = (PowerManager) context.getSystemService(Activity.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG+"_Lock");
            wakeLock.acquire();

            downloadedFile = new File(downloadsDir, fileName);
            URL url = new URL(this.url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.connect();
            fos = new FileOutputStream(downloadedFile);
            in = conn.getInputStream();

            byte[] buf = new byte[1024 * 8];
            long bytesToRead = fileSize;
            int bytesRead;
            while (bytesToRead > 0 && (bytesRead = in.read(buf, 0, (int) Math.min(buf.length, bytesToRead))) != -1 && !isCancelled()) {
                fos.write(buf, 0, bytesRead);
                publishProgress(bytesRead);
                bytesToRead -= bytesRead;
            }

        } catch (Exception e) {
            Log.e(TAG,"Error while download file form url:" + url, e);
        } finally {
            try {
                if(fos!=null)
                    fos.close();
                if(in!=null)
                    in.close();
            } catch (IOException ignored) {}

            if(wakeLock.isHeld())
                wakeLock.release();
        }

        return downloadedFile;
    }

    public void setListener(PostDownloadCallBackListener listener) {
        this.listener = listener;
    }

    public interface PostDownloadCallBackListener {
        void doCallBack(File file);
    }
}
