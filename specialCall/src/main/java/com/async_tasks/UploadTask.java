package com.async_tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.actions.ActionFactory;
import com.actions.ClientAction;
import com.app.AppStateManager;
import com.data_objects.Constants;
import com.data_objects.KeysForBundle;
import com.mediacallz.app.R;
import com.utils.BroadcastUtils;
import com.utils.NetworkingUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import ClientObjects.ConnectionToServer;
import ClientObjects.IServerProxy;
import DataObjects.DataKeys;
import DataObjects.SharedConstants;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;
import MessagesToClient.MessageToClient;
import MessagesToServer.MessageToServer;
import MessagesToServer.ServerActionType;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 11/08/2016.
 */
public class UploadTask extends AsyncTask<Void, Integer, Void> implements IServerProxy {
    private final String TAG = UploadTask.class.getSimpleName();
    private ConnectionToServer connectionToServer;
    private HashMap data;
    private ProgressDialog progDialog;
    private UploadTask taskInstance;
    private BufferedInputStream _bis;
    private PowerManager.WakeLock wakeLock;
    private FileManager fileForUpload;
    private Context context;
    private boolean fileUploadSuccess = false;

    public UploadTask(Context context, Bundle bundle) {
        this.context = context;
        fileForUpload = (FileManager) bundle.get(KeysForBundle.FILE_FOR_UPLOAD);

        HashMap<DataKeys, Object> data = getDataForUpload(bundle);

        connectionToServer = new ConnectionToServer(
                SharedConstants.STROAGE_SERVER_HOST,
                SharedConstants.STORAGE_SERVER_PORT,
                this);
        this.data = data;
        taskInstance = this;

    }

    @NonNull
    private HashMap<DataKeys, Object> getDataForUpload(Bundle bundle) {
        HashMap<DataKeys, Object> data = new HashMap<>();
        String myId = Constants.MY_ID(context);
        double appVersion = Constants.APP_VERSION();

        data.put(DataKeys.APP_VERSION, appVersion);
        data.put(DataKeys.SOURCE_ID, myId);
        data.put(DataKeys.SOURCE_LOCALE, Locale.getDefault().getLanguage());
        data.put(DataKeys.DESTINATION_ID, bundle.get(KeysForBundle.DEST_ID));
        data.put(DataKeys.DESTINATION_CONTACT_NAME, bundle.get(KeysForBundle.DEST_NAME));
        data.put(DataKeys.MD5, fileForUpload.getMd5());
        data.put(DataKeys.MANAGED_FILE, fileForUpload);
        data.put(DataKeys.EXTENSION, fileForUpload.getFileExtension());
        data.put(DataKeys.FILE_PATH_ON_SRC_SD, fileForUpload.getFile().getAbsolutePath());
        data.put(DataKeys.FILE_SIZE, fileForUpload.getFileSize());
        data.put(DataKeys.FILE_TYPE, fileForUpload.getFileType());
        data.put(DataKeys.SPECIAL_MEDIA_TYPE, bundle.get(KeysForBundle.SPEC_MEDIA_TYPE));
        data.put(DataKeys.SOURCE_WITH_EXTENSION, myId + "." + fileForUpload.getFileExtension());
        return data;
    }

    @Override
    protected void onPreExecute() {

        String cancel = context.getResources().getString(R.string.cancel);

        progDialog = new ProgressDialog(context,R.style.AlertDialogCustom);
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

        if(NetworkingUtils.isNetworkAvailable(context))
            executeFileUpload();
        else
            reportNoInternet();

        return null;
    }

    private void reportNoInternet() {
        BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.NO_INTERNET));
    }

    private void executeFileUpload() {
        MessageToServer msgUF = new MessageToServer(ServerActionType.UPLOAD_FILE_V2, Constants.MY_ID(context), data);

        DataOutputStream dos;
        try {

            PowerManager powerManager = (PowerManager) context.getSystemService(Activity.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "UploadTask_Lock");
            wakeLock.acquire();

            connectionToServer.openConnection();
            connectionToServer.sendToServer(msgUF);

            log(Log.INFO, TAG, "Initiating file data upload. [Filepath]: " + fileForUpload.getFile().getAbsolutePath());

            dos = new DataOutputStream(connectionToServer.getClientSocket().getOutputStream());

            FileInputStream fis = new FileInputStream(fileForUpload.getFile());
            _bis = new BufferedInputStream(fis);

            byte[] buf = new byte[1024 * 8];
            long bytesToRead = fileForUpload.getFileSize();
            int bytesRead;
            while (bytesToRead > 0 && (bytesRead = _bis.read(buf, 0, (int) Math.min(buf.length, bytesToRead))) != -1 && !isCancelled()) {
                dos.write(buf, 0, bytesRead);
                publishProgress((int) bytesRead);
                bytesToRead -= bytesRead;
            }

            try {
                Thread.sleep(1000); // Sleeping so in fast uploads the dialog won't appear and disappear too fast (like a blink)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            fileUploadSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Failed:" + e.getMessage());
            BroadcastUtils.sendEventReportBroadcast(context, TAG,
                    new EventReport(EventType.STORAGE_ACTION_FAILURE));
        } finally {

            try {
                connectionToServer.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (wakeLock.isHeld())
                wakeLock.release();
        }
    }

    @Override
    protected void onCancelled() {

        if (_bis != null) {
            try {
                _bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            connectionToServer.closeConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        if (progDialog != null && progDialog.isShowing()) {
            progDialog.dismiss();
        }
        if(fileUploadSuccess) {
            reportSuccess();
            waitingForTransferSuccess();
        }
    }

    private void reportSuccess() {
        String msg = context.getResources().getString(R.string.upload_success);
        // Setting state
        AppStateManager.setAppState(context, TAG, AppStateManager.STATE_READY);

        // Setting parameters for snackbar message
        int color = Color.GREEN;
        int sBarDuration = Snackbar.LENGTH_LONG;

        UI_Utils.showSnackBar(msg, color, sBarDuration, false, context);
    }

    @Override
    public void handleMessageFromServer(MessageToClient msg, ConnectionToServer connectionToServer) {

        try {

            ClientAction clientAction = ActionFactory.instance().getAction(msg.getActionType());
            clientAction.setConnectionToServer(connectionToServer);
            EventReport eventReport = clientAction.doClientAction(msg.getData());

            if(eventReport==null)
                log(Log.WARN, TAG, "ClientAction:" + clientAction.getClass().getSimpleName() + " returned null eventReport");
            else if (eventReport.status() != EventType.NO_ACTION_REQUIRED)
                BroadcastUtils.sendEventReportBroadcast(context, TAG, eventReport);

        } catch (Exception e) {
            String errMsg = "Handling message from server failed. Reason:" + e.getMessage();
            log(Log.INFO, TAG, errMsg);
        } finally {

            // Finished handling request-response transaction
            try {
                connectionToServer.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleDisconnection(ConnectionToServer cts, String errMsg) {
        // Ignoring. We don't wait for response from server on upload anyway
    }

    private void waitingForTransferSuccess() {
        if (!SharedPrefUtils.getBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_UPLOAD_DIALOG)) {
            UI_Utils.showWaitingForTranferSuccussDialog(context, "MainActivity", context.getResources().getString(R.string.sending_to_contact)
                    , context.getResources().getString(R.string.waiting_for_transfer_sucess_dialog_msg));
        }
    }
}
