package com.async_tasks;

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

import com.actions.ActionFactory;
import com.actions.ClientAction;
import com.app.AppStateManager;
import com.client.ConnectionToServer;
import com.client.IServerProxy;
import com.client.ProgressListener;
import com.client.ProgressiveEntity;
import com.data_objects.Constants;
import com.google.gson.reflect.TypeToken;
import com.mediacallz.app.R;
import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.lang.reflect.Type;
import java.util.Locale;

import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;
import MessagesToClient.MessageToClient;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.entity.mime.HttpMultipartMode;
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder;
import cz.msebera.android.httpclient.entity.mime.content.FileBody;

import static com.crashlytics.android.Crashlytics.log;
import static com.data_objects.KeysForBundle.DEST_ID;
import static com.data_objects.KeysForBundle.DEST_NAME;
import static com.data_objects.KeysForBundle.FILE_FOR_UPLOAD;
import static com.data_objects.KeysForBundle.SPEC_MEDIA_TYPE;

/**
 * Created by Mor on 11/08/2016.
 */
public class UploadTask extends AsyncTask<Void, Integer, Void> implements IServerProxy, ProgressListener {
    private static final String URL_UPLOAD = "http://" + Constants.SERVER_HOST + ":" + Constants.SERVER_PORT + "/v1/UploadFile";
    private static final Type RESPONSE_TYPE = new TypeToken<MessageToClient<EventReport>>() {
    }.getType();
    private final String TAG = UploadTask.class.getSimpleName();
    private ConnectionToServer connectionToServer;
    private ProgressDialog progDialog;
    private UploadTask taskInstance;
    private PowerManager.WakeLock wakeLock;
    private Context context;
    private FileManager fileForUpload;
    private ProgressiveEntity progressiveEntity;

    public UploadTask(Context context, Bundle bundle) {
        this.context = context;
        fileForUpload = (FileManager) bundle.get(FILE_FOR_UPLOAD);

        progressiveEntity = prepareProgressiveEntity(bundle);

        connectionToServer = new ConnectionToServer(this, RESPONSE_TYPE);
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
        progDialog.setMax((int) fileForUpload.getFileSize());
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
            connectionToServer.sendMultipartToServer(URL_UPLOAD, progressiveEntity);

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
        }

        return null;
    }

    @Override
    protected void onCancelled() {
        connectionToServer.closeConnection();
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

        String msg = context.getResources().getString(R.string.upload_success);
        // Setting state
        AppStateManager.setAppState(context, TAG, AppStateManager.STATE_READY);

        // Setting parameters for snackbar message
        int color = Color.GREEN;
        int sBarDuration = Snackbar.LENGTH_LONG;

        UI_Utils.showSnackBar(msg, color, sBarDuration, false, context);

        waitingForTransferSuccess();
    }

    @Override
    public void handleMessageFromServer(MessageToClient msg, ConnectionToServer connectionToServer) {

        ClientAction clientAction = null;
        try {

            clientAction = ActionFactory.instance().getAction(msg.getActionType());
            clientAction.setConnectionToServer(connectionToServer);
            EventReport eventReport = clientAction.doClientAction(msg.getResult());

            if (eventReport == null)
                log(Log.WARN, TAG, "ClientAction:" + clientAction.getClass().getSimpleName() + " returned null eventReport");
            else if (eventReport.status() != EventType.NO_ACTION_REQUIRED)
                BroadcastUtils.sendEventReportBroadcast(context, TAG, eventReport);

        } catch (Exception e) {
            String errMsg;
            if (clientAction == null)
                errMsg = "Handling message from server failed. ClientAction was null. Message:" + msg;
            else
                errMsg = "Handling message from server failed. ClientAction:" + clientAction.getClass().getSimpleName() + " Reason:" + e.getMessage();
            log(Log.ERROR, TAG, errMsg);
        } finally {

            // Finished handling request-response transaction
            connectionToServer.closeConnection();
        }
    }

    @Override
    public void handleDisconnection(ConnectionToServer cts, String errMsg) {
        log(Log.ERROR, TAG, "Failed:" + errMsg);
        BroadcastUtils.sendEventReportBroadcast(context, TAG,
                new EventReport(EventType.STORAGE_ACTION_FAILURE));
    }

    @Override
    public void reportProgress(int bytesWritten) {
        publishProgress(bytesWritten);
    }

    private ProgressiveEntity prepareProgressiveEntity(Bundle bundle) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        FileBody fb = new FileBody(fileForUpload.getFile());
        builder.addPart("fileForUpload", fb);
        prepareDataForUpload(builder, bundle);
        HttpEntity httpEntity = builder.build();
        return new ProgressiveEntity(httpEntity, this);
    }

    private void prepareDataForUpload(MultipartEntityBuilder builder, Bundle bundle) {
        String myId = Constants.MY_ID(context);
        double appVersion = Constants.APP_VERSION();

        builder.addTextBody(DataKeys.MESSAGE_INITIATER_ID.toString(), myId);
        builder.addTextBody(DataKeys.APP_VERSION.toString(), String.valueOf(appVersion));
        builder.addTextBody(DataKeys.SOURCE_ID.toString(), myId);
        builder.addTextBody(DataKeys.SOURCE_LOCALE.toString(), Locale.getDefault().getLanguage());
        builder.addTextBody(DataKeys.DESTINATION_ID.toString(), bundle.get(DEST_ID).toString());
        builder.addTextBody(DataKeys.DESTINATION_CONTACT_NAME.toString(), bundle.get(DEST_NAME).toString());
        builder.addTextBody(DataKeys.MD5.toString(), fileForUpload.getMd5());
        builder.addTextBody(DataKeys.EXTENSION.toString(), fileForUpload.getFileExtension());
        builder.addTextBody(DataKeys.FILE_PATH_ON_SRC_SD.toString(), fileForUpload.getFile().getAbsolutePath());
        builder.addTextBody(DataKeys.FILE_SIZE.toString(), String.valueOf(fileForUpload.getFileSize()));
        builder.addTextBody(DataKeys.FILE_TYPE.toString(), fileForUpload.getFileType().toString());
        builder.addTextBody(DataKeys.SPECIAL_MEDIA_TYPE.toString(), bundle.get(SPEC_MEDIA_TYPE).toString());
        builder.addTextBody(DataKeys.SOURCE_WITH_EXTENSION.toString(), myId + "." + fileForUpload.getFileExtension());
    }

    private void waitingForTransferSuccess() {
        if (!SharedPrefUtils.getBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_UPLOAD_DIALOG)) {
            UI_Utils.showWaitingForTranferSuccussDialog(context, "MainActivity", context.getResources().getString(R.string.sending_to_contact)
                    , context.getResources().getString(R.string.waiting_for_transfer_sucess_dialog_msg));
        }
    }
}
