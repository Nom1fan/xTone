package com.async.tasks;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
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

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT)
            progressDialog = new ProgressDialog(context,R.style.AlertDialogCustom);
        else
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
    protected void onProgressUpdate(Integer... progress) {

        if (progressDialog != null) {
            progressDialog.incrementProgressBy(progress[0]);
        }
    }

    @Override
    protected void onPostExecute(File downloadedFile) {
        dismissWithCheck(progressDialog);
        if(listener!=null)
            listener.doCallBack(downloadedFile);
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
            long bytesToRead = fileSize ;
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

    private void dismissWithCheck(Dialog dialog) {
        if (dialog != null) {
            if (dialog.isShowing()) {

                //get the Context object that was used to great the dialog
                Context context = ((ContextWrapper) dialog.getContext()).getBaseContext();

                // if the Context used here was an activity AND it hasn't been finished or destroyed
                // then dismiss it
                if (context instanceof Activity) {

                    // Api >=17
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        if (!((Activity) context).isFinishing() && !((Activity) context).isDestroyed()) {
                            dismissWithTryCatch(dialog);
                        }
                    } else {

                        // Api < 17. Unfortunately cannot check for isDestroyed()
                        if (!((Activity) context).isFinishing()) {
                            dismissWithTryCatch(dialog);
                        }
                    }
                } else
                    // if the Context used wasn't an Activity, then dismiss it too
                    dismissWithTryCatch(dialog);
            }
            dialog = null;
        }
    }

    private void dismissWithTryCatch(Dialog dialog) {
        try {
            dialog.dismiss();
        } catch (final Exception e) {
            // Do nothing.
        } finally {
            dialog = null;
        }
    }
}
