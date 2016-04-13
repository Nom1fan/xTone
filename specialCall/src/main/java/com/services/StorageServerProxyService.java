package com.services;

import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.async_tasks.UploadTask;
import com.data_objects.Constants;
import com.utils.BroadcastUtils;
import com.utils.ContactsUtils;
import com.utils.FileCompressorUtil;

import java.io.File;
import java.io.IOException;

import ClientObjects.ConnectionToServer;
import DataObjects.PushEventKeys;
import DataObjects.SharedConstants;
import DataObjects.SpecialMediaType;
import DataObjects.TransferDetails;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;
import MessagesToServer.MessageClearMedia;
import MessagesToServer.MessageNotifyMediaCleared;
import MessagesToServer.MessageRequestDownload;


/**
 * <pre>
 * A Proxy that manages storage server operations.
 * Provided operations:
 * - Download file
 * - Upload file
 *
 * @author Mor
 */
public class StorageServerProxyService extends AbstractServerProxy {

    //region members
    private static UploadTask _uploadTask;
    //endregion

    //region Service actions
    public static final String ACTION_DOWNLOAD = "com.services.StorageServerProxyService.DOWNLOAD";
    public static final String ACTION_UPLOAD = "com.services.StorageServerProxyService.UPLOAD";
    public static final String ACTION_NOTIFY_MEDIA_CLEARED = "com.services.StorageServerProxyService.NOTIFY_MEDIA_CLEARED";
    public static final String ACTION_CLEAR_MEDIA = "com.services.StorageServerProxyService.CLEAR_MEDIA";
    //endregion

    //region Service intent keys
    public static final String FILE_TO_UPLOAD       =   "FILE_TO_UPLOAD";
    public static final String DESTINATION_ID       =   "DESTINATION_ID";
    public static final String SPECIAL_MEDIA_TYPE   =   "SPECIAL_MEDIA_TYPE";
    public static final String TRANSFER_DETAILS     =   "CALL_RECORD";
    //endregion

    public StorageServerProxyService() {
        super(StorageServerProxyService.class.getSimpleName());
    }

    //region Service methods
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "Started");

        boolean shouldStop = handleCrashedService(flags, startId);
        if (shouldStop)
            return START_REDELIVER_INTENT;

        final Intent intentForThread = intent;

        new Thread() {
            @Override
            public void run() {

                if (intentForThread != null) {
                    String action = intentForThread.getAction();
                    Log.i(TAG, "Action:" + action);

                    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

                    try {
                        switch (action) {
                            case ACTION_DOWNLOAD:
                                actionDownload(intentForThread, powerManager);
                                break;

                            case ACTION_UPLOAD:
                                actionUpload(intentForThread, powerManager);
                                break;

                            case ACTION_CLEAR_MEDIA:
                                actionClear(intentForThread);
                                break;

                            case ACTION_NOTIFY_MEDIA_CLEARED:
                                actionNotifyMediaCleared(intentForThread);
                                break;

                            default:
                                setMidAction(false);
                                Log.w(TAG, "Service started with invalid action:" + action);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        String errMsg = "Action failed:" + action + " Exception:" + e.getMessage();
                        handleActionFailed();
                        Log.e(TAG, errMsg);
                        //handleDisconnection(errMsg); //TODO Maybe no need for this?
                    } catch (Exception e) {
                        e.printStackTrace();
                        String errMsg = "Action failed:" + action + " Exception:" + e.getMessage();
                        handleActionFailed();
                        Log.e(TAG, errMsg);
                    }
                } else
                    Log.w(TAG, "Service started with missing action");
            }
        }.start();

        markCrashedServiceHandlingComplete(flags, startId);

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //endregion

    //region Action methods
    private void actionDownload(Intent intent, PowerManager powerManager) throws IOException {

        setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + "_wakeLock");
        wakeLock.acquire();
        TransferDetails td = (TransferDetails) intent.getSerializableExtra(PushEventKeys.PUSH_DATA);
        requestDownloadFromServer(openSocket(SharedConstants.STROAGE_SERVER_HOST, SharedConstants.STORAGE_SERVER_PORT), td);
    }

    private void actionUpload(Intent intent, PowerManager powerManager) throws IOException {

        setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + "_wakeLock");
        wakeLock.acquire();

        String destId = intent.getStringExtra(DESTINATION_ID);
        SpecialMediaType specialMediaType = (SpecialMediaType) intent.getSerializableExtra(SPECIAL_MEDIA_TYPE);
        String tempCompressFolder = Constants.TEMP_COMPRESSED_FOLDER + destId;
        File tempCompressFolderDir = new File(tempCompressFolder);
        tempCompressFolderDir.mkdirs();

        ConnectionToServer connectionToServer = openSocket(SharedConstants.STROAGE_SERVER_HOST, SharedConstants.STORAGE_SERVER_PORT);
        FileManager managedFile = (FileManager) intent.getSerializableExtra(FILE_TO_UPLOAD);
        managedFile = FileCompressorUtil.compressFileIfNecessary(managedFile, tempCompressFolder, getApplicationContext());
        uploadFileToServer(connectionToServer, destId, managedFile, specialMediaType);
        releaseLockIfNecessary();

    }

    private void actionClear(Intent intent) throws IOException {

        setMidAction(true);
        ConnectionToServer connectionToServer = openSocket(SharedConstants.STROAGE_SERVER_HOST, SharedConstants.STORAGE_SERVER_PORT);
        String destId = intent.getStringExtra(DESTINATION_ID);
        SpecialMediaType specialMediaType = (SpecialMediaType) intent.getSerializableExtra(SPECIAL_MEDIA_TYPE);
        sendClearCommandToServer(connectionToServer, destId, specialMediaType);
    }

    private void actionNotifyMediaCleared(Intent intent) throws IOException {

        setMidAction(true);
        ConnectionToServer connectionToServer = openSocket(SharedConstants.STROAGE_SERVER_HOST, SharedConstants.STORAGE_SERVER_PORT);
        TransferDetails td = (TransferDetails) intent.getSerializableExtra(TRANSFER_DETAILS);
        MessageNotifyMediaCleared msgNMC = new MessageNotifyMediaCleared(Constants.MY_ID(getApplicationContext()), td);
        connectionToServer.sendToServer(msgNMC);
    }

    private void handleActionFailed() {

        BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.STORAGE_ACTION_FAILURE, null ,null));
    }
    //endregion

    //region Storage operations methods

    /**
     * Sends a clear media command to the server
     *
     * @param connectionToServer
     * @param destId             The destination id of the user for which the media will be cleared
     * @param specialMediaType   The special media type of the media to clear
     * @throws IOException
     */
    private void sendClearCommandToServer(
            ConnectionToServer connectionToServer,
            String destId,
            SpecialMediaType specialMediaType) throws IOException {

        TransferDetails td = new TransferDetails(Constants.MY_ID(getApplicationContext()), destId, specialMediaType);
        MessageClearMedia msgCM = new MessageClearMedia(td.getSourceId(), td);
        connectionToServer.sendToServer(msgCM);
    }

    /**
     * Uploads a file to the server, sending it to a destination number
     *
     * @param connectionToServer
     * @param destNumber         The destination number to whom the file is for
     * @param managedFile        The file to upload inside a manager wrapper
     * @param specialMediaType   The special media type of the file to upload
     * @throws IOException
     */
    private void uploadFileToServer(
            ConnectionToServer connectionToServer,
            String destNumber,
            FileManager managedFile,
            SpecialMediaType specialMediaType) throws IOException {

        TransferDetails td = new TransferDetails(
                Constants.MY_ID(getApplicationContext()),
                destNumber,
                ContactsUtils.getContactName(getApplicationContext(), destNumber),
                managedFile,
                specialMediaType);

        _uploadTask = new UploadTask(connectionToServer, td);
        BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.UPLOADING, null, null));
    }

    /**
     * Requests a download from the server
     *
     * @param connectionToServer
     * @param td                 - The transfer details
     */
    private void requestDownloadFromServer(ConnectionToServer connectionToServer, TransferDetails td) throws IOException {

        MessageRequestDownload msgRD = new MessageRequestDownload(td);
        connectionToServer.sendToServer(msgRD);
    }
    //endregion

    //region Getters
    public static UploadTask getUploadTask() {

        return _uploadTask;
    }
    //endregion
}