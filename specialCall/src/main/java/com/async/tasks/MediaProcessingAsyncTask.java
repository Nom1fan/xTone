package com.async.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.data.objects.Constants;
import com.flows.UploadFileFlowListener;
import com.mediacallz.app.R;
import com.netcompss.ffmpeg4android.ProgressCalculator;
import com.netcompss.loader.LoadJNI;
import com.utils.BroadcastUtils;
import com.utils.MediaFileProcessingUtils;

import com.event.EventReport;
import com.event.EventType;
import com.files.media.MediaFile;
import com.utils.MediaFileUtils;
import com.utils.UtilityFactory;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 20/07/2016.
 */
public abstract class MediaProcessingAsyncTask extends AsyncTask<Bundle, Integer, Bundle> {

    protected final String TAG = TrimTask.class.getSimpleName();
    protected final String OUT_FOLDER = Constants.COMPRESSED_FOLDER;
    protected final String AUDIO_OUT_FOLDER = Constants.AUDIO_HISTORY_FOLDER;
    protected MediaProcessingAsyncTask instance = this;
    protected MediaFile baseFile;
    protected boolean calcProgress = false;
    protected MediaFile processedFile;
    protected Bundle bundle;
    protected UploadFileFlowListener uploadFileFlow;
    protected String processedFilePath;
    protected Thread workerThread;
    protected Thread updateProgressThread;
    protected ProgressDialog progressDialog;
    protected Context context;
    protected int order;
    protected LoadJNI vk = new LoadJNI();
    protected final Object lock = new Object();
    protected volatile boolean contCalcProgress = false;
    protected volatile boolean updateThreadNextIterStarted = false;
    protected MediaFileProcessingUtils mediaFileProcessingUtils = new MediaFileProcessingUtils(vk);
    protected MediaFileUtils mediaFileUtils = UtilityFactory.getUtility(MediaFileUtils.class);

    public MediaProcessingAsyncTask(int order,
                                    UploadFileFlowListener uploadFileFlow,
                                    Context context) {
        this.order=order;
        this.uploadFileFlow=uploadFileFlow;
        this.context=context;
    }

    @Override
    protected Bundle doInBackground(Bundle... params) {
        return null;
    }

    public abstract boolean isProcessingNeeded(Context ctx, MediaFile baseFile);

    protected void sendLoadingCancelled(Context ctx, String tag) {
        BroadcastUtils.sendEventReportBroadcast(ctx, tag, new EventReport(EventType.LOADING_CANCEL));
    }

    protected String getProcessedFileName(MediaFile baseFile, String action) {
        String nameWithoutExtension = mediaFileUtils.getNameWithoutExtension(baseFile);
        String procFilePath = baseFile.getMd5() + "_" + nameWithoutExtension + "_" + action + "." + baseFile.getExtension();
        return procFilePath;
    }

    protected void startUpdateProgressThread() {
        updateProgressThread = new Thread(new Runnable() {

            ProgressCalculator pc = new ProgressCalculator(MediaFileProcessingUtils.VK_LOG_PATH);

            @Override
            public void run() {
                Log.d(TAG, "Progress update started");
                int progress;
                try {
                    contCalcProgress = true;
                    while (contCalcProgress) {
                        Thread.sleep(300);
                        progress = pc.calcProgress();
                        if (progress != 0 && progress < 100) {
                            log(Log.INFO, TAG, "Progress update thread. Progress is:" + progress + "%");
                            progressDialog.setProgress(progress);
                        } else if (progress == 100) {
                            log(Log.INFO, TAG, "Progress is 100, exiting progress update thread");
                            progressDialog.setProgress(100);
                            pc.initCalcParamsForNextInter();

                            // Waiting for next iteration
                            synchronized (lock) {
                                lock.wait();
                                while (!updateThreadNextIterStarted)
                                    lock.wait();
                            }
                        }
                    }
                    progressDialog.setProgress(100);
                } catch (Exception e) {
                    log(Log.ERROR, TAG, e.getMessage());
                }
            }
        });

        updateProgressThread.start();
    }

    protected Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            log(Log.INFO, TAG, "Handler got message:" + msg.what);

            // Stopping the transcoding native
            if (msg.what == MediaFileProcessingUtils.STOP_TRANSCODING_MSG) {
                log(Log.INFO, TAG, "Got cancel message, calling fexit");
                if (progressDialog != null)
                    progressDialog.dismiss();

                vk.fExit(context);

                wakeUpdateThreadToFinish();
            } else if (msg.what == MediaFileProcessingUtils.FINISHED_TRANSCODING_MSG) {

                wakeUpdateThreadToFinish();

                if (progressDialog != null)
                    progressDialog.dismiss();
            } else if (msg.what == MediaFileProcessingUtils.COMPRESSION_PHASE_2) {

                log(Log.INFO, TAG, "Got compression phase 2 message");

                if (progressDialog != null) {

                    String str = context.getResources().getString(R.string.compressing_file2);
                    progressDialog.setProgress(0);
                    progressDialog.setTitle(str);
                }

                wakeUpdateThreadToContinue();
            }
        }

        private void wakeUpdateThreadToFinish() {

            contCalcProgress = false;
            updateThreadNextIterStarted = true;
            synchronized (lock) {
                lock.notify();
            }
        }

        private void wakeUpdateThreadToContinue() {

            updateThreadNextIterStarted = true;
            synchronized (lock) {
                lock.notify();
            }
        }
    };
}
