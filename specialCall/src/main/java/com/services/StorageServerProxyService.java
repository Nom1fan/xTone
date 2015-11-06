package com.services;

import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import com.async_tasks.UploadTask;
import com.data_objects.Constants;
import com.utils.NotificationUtils;
import java.io.IOException;
import ClientObjects.ConnectionToServer;
import ClientObjects.IServerProxy;
import DataObjects.PushEventKeys;
import DataObjects.SharedConstants;
import DataObjects.TransferDetails;
import FilesManager.FileManager;
import MessagesToServer.MessageRequestDownload;


/**
 * <pre>
 * A Proxy that manages storage server operations.
 * Provided operations:
 * - Download file
 * - Upload file
 * @author Mor
 */
public class StorageServerProxyService extends AbstractServerProxy implements IServerProxy {

    // Service actions
    public static final String ACTION_DOWNLOAD = "com.services.StorageServerProxyService.DOWNLOAD";
    public static final String ACTION_UPLOAD = "com.services.StorageServerProxyService.UPLOAD";

    // Service intent keys
    public static final String FILE_TO_UPLOAD = "com.services.StorageServerProxyService.FILE_TO_UPLOAD";
    public static final String DESTINATION_ID = "com.services.StorageServerProxyService.DESTINATION_ID";


    public StorageServerProxyService() {
        super(StorageServerProxyService.class.getSimpleName());
    }

    /* Service overriding methods */

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "StorageServerProxyService started");

        final Intent intentForThread = intent;

        new Thread() {

            @Override
            public void run() {
                if (intentForThread != null)
                {
                    String action = intentForThread.getAction();
                    Log.i(TAG, "Action:" + action);

                    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

                    try {

                        switch (action) {

                            case ACTION_DOWNLOAD: {

                                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + "_wakeLock");
                                wakeLock.acquire();
                                TransferDetails td = (TransferDetails) intentForThread.getSerializableExtra(PushEventKeys.PUSH_DATA);
                                requestDownloadFromServer(openSocket(SharedConstants.STROAGE_SERVER_HOST, SharedConstants.STORAGE_SERVER_PORT), td);
                            }
                                break;

                            case ACTION_UPLOAD: {

                                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + "_wakeLock");
                                wakeLock.acquire();
                                String destId = intentForThread.getStringExtra(DESTINATION_ID);
                                ConnectionToServer connectionToServer = openSocket(SharedConstants.STROAGE_SERVER_HOST, SharedConstants.STORAGE_SERVER_PORT);
                                FileManager managedFile = (FileManager) intentForThread.getSerializableExtra(FILE_TO_UPLOAD);
                                uploadFileToServer(connectionToServer, destId, managedFile);
                                releaseLockIfNecessary();
                            }
                            break;


                            default:
                                Log.w(TAG, "Service started with invalid action:" + action);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        String errMsg = "Action failed:"+action+" Exception:"+e.getMessage();
                        Log.e(TAG, errMsg);
                        handleDisconnection(errMsg);
                    }
                } else
                    Log.w(TAG, "Service started with missing action");


            }
        }.start();

        return START_NOT_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

          /* IServerProxy operations methods */

    /**
     * Uploads a file to the server, sending it to a destination number
     * @param managedFile   - The file to upload inside a manager wrapper
     * @param destNumber - The destination number to whom the file is for
     */
    public void uploadFileToServer(ConnectionToServer connectionToServer, final String destNumber, final FileManager managedFile) throws IOException {

        TransferDetails td = new TransferDetails(Constants.MY_ID(mContext), destNumber, managedFile);
        NotificationUtils.createHelper(mContext, "File upload to:" + td.getDestinationId() + " is pending");
        new UploadTask(mContext, connectionToServer, td).execute();
    }

    /**
     * Requests a download from the server
     * @param connectionToServer
     * @param td - The transfer details
     */
    public void requestDownloadFromServer(ConnectionToServer connectionToServer, TransferDetails td) throws IOException {

        MessageRequestDownload msgRD = new MessageRequestDownload(td);
        connectionToServer.sendToServer(msgRD);

    }

}