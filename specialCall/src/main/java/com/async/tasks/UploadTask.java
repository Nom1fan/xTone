package com.async.tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.app.AppStateManager;
import com.client.ConnectionToServer;
import com.client.ProgressListener;
import com.client.ProgressiveEntity;
import com.data.objects.Constants;
import com.enums.SpecialMediaType;
import com.event.EventReport;
import com.event.EventType;
import com.files.media.MediaFile;
import com.google.gson.Gson;
import com.mediacallz.app.R;
import com.model.request.UploadFileRequest;
import com.utils.BroadcastUtils;
import com.utils.RequestUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.nio.charset.Charset;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.mime.HttpMultipartMode;
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder;
import cz.msebera.android.httpclient.entity.mime.content.FileBody;
import cz.msebera.android.httpclient.entity.mime.content.StringBody;

import static com.crashlytics.android.Crashlytics.log;
import static com.data.objects.KeysForBundle.DEST_ID;
import static com.data.objects.KeysForBundle.DEST_NAME;
import static com.data.objects.KeysForBundle.FILE_FOR_UPLOAD;
import static com.data.objects.KeysForBundle.SPEC_MEDIA_TYPE;

/**
 * Created by Mor on 11/08/2016.
 */
public class UploadTask extends AsyncTask<Void, Integer, Void> implements ProgressListener {
    private static final String URL_UPLOAD = "http://" + Constants.SERVER_HOST + ":" + Constants.SERVER_PORT + "/v1/UploadFile";
    private final String TAG = UploadTask.class.getSimpleName();
    private ConnectionToServer connectionToServer;
    private ProgressDialog progDialog;
    private UploadTask taskInstance;
    private PowerManager.WakeLock wakeLock;
    private Context context;
    private MediaFile fileForUpload;
    private ProgressiveEntity progressiveEntity;
    private boolean isOK = true;

    public UploadTask(Context context, Bundle bundle) {
        this.context = context;
        fileForUpload = (MediaFile) bundle.get(FILE_FOR_UPLOAD);
        progressiveEntity = prepareProgressiveEntity(bundle);
        connectionToServer = new ConnectionToServer();
        taskInstance = this;
    }

    @Override
    protected void onPreExecute() {

        String cancel = context.getResources().getString(R.string.cancel);

        progDialog = new ProgressDialog(context);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(false);
        progDialog.setTitle(context.getResources().getString(R.string.uploading));
        progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progDialog.setProgress(0);
        progDialog.setMax((int) fileForUpload.getSize());
        progDialog.setButton(DialogInterface.BUTTON_NEGATIVE, cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                taskInstance.cancel(true);
            }
        });

        progDialog.show();
    }

    @Override
    protected Void doInBackground(Void... voids) {

        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Activity.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "UploadTask_Lock");
            wakeLock.acquire();

            log(Log.INFO, TAG, "Initiating file data upload. [Filepath]: " + fileForUpload.getFile().getAbsolutePath());
            int responseCode = connectionToServer.sendMultipartToServer(URL_UPLOAD, progressiveEntity);
            if (responseCode != HttpStatus.SC_OK) {
                isOK = false;
            }

            try {
                Thread.sleep(1000); // Sleeping so in fast uploads the dialog won't appear and disappear too fast (like a blink)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (progDialog != null && progDialog.isShowing()) {
                progDialog.dismiss();
            }
        } finally {
            if (wakeLock.isHeld())
                wakeLock.release();
            connectionToServer.disconnect();
        }

        return null;
    }

    @Override
    protected void onCancelled() {
        connectionToServer.disconnect();
        BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.LOADING_CANCEL));
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (progDialog != null) {
            progDialog.incrementProgressBy(progress[0]);
        }
    }

    @Override
    protected void onPostExecute(Void result) {

        if(isOK) {
            String msg = context.getResources().getString(R.string.upload_success);
            // Setting state
            AppStateManager.setAppState(context, TAG, AppStateManager.STATE_READY);

            // Setting parameters for snackbar message
            int color = Color.GREEN;
            int sBarDuration = Snackbar.LENGTH_LONG;

            UI_Utils.showSnackBar(msg, color, sBarDuration, false, context);

            waitingForTransferSuccess();
        }
        else {
            BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.STORAGE_ACTION_FAILURE));
        }
    }


    @Override
    public void reportProgress(int bytesWritten) {
        publishProgress(bytesWritten);
    }

    private ProgressiveEntity prepareProgressiveEntity(Bundle bundle) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.setCharset(Charset.defaultCharset());
        FileBody fb = new FileBody(fileForUpload.getFile());
        builder.addPart("fileForUpload", fb);
        builder.addPart("jsonPart", new StringBody(prepareDataForUpload(bundle), ContentType.TEXT_PLAIN.withCharset("UTF-8")));
        HttpEntity httpEntity = builder.build();
        return new ProgressiveEntity(httpEntity, this);
    }

    private String prepareDataForUpload(Bundle bundle) {
        String myId = Constants.MY_ID(context);
        UploadFileRequest uploadFileRequest = new UploadFileRequest();
        RequestUtils.prepareDefaultRequest(context, uploadFileRequest);
        uploadFileRequest.setSourceId(myId);
        uploadFileRequest.setLocale(Locale.getDefault().getLanguage());
        uploadFileRequest.setDestinationId(bundle.get(DEST_ID).toString());
        uploadFileRequest.setDestinationContactName(bundle.get(DEST_NAME).toString());
        uploadFileRequest.setMediaFile(fileForUpload);
        uploadFileRequest.setFilePathOnSrcSd(fileForUpload.getFile().getAbsolutePath());
        uploadFileRequest.setSpecialMediaType((SpecialMediaType) bundle.get(SPEC_MEDIA_TYPE));
        return new Gson().toJson(uploadFileRequest);
    }

    private void waitingForTransferSuccess() {
        if (!SharedPrefUtils.getBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_UPLOAD_DIALOG)) {
            UI_Utils.showWaitingForTranferSuccussDialog(context, "MainActivity", context.getResources().getString(R.string.sending_to_contact)
                    , context.getResources().getString(R.string.waiting_for_transfer_sucess_dialog_msg));
        }
    }
}
