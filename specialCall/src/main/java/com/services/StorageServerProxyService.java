package com.services;

import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.client.ConnectionToServer;
import com.data_objects.Constants;
import com.utils.BroadcastUtils;
import com.utils.CollectionsUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import DataObjects.DataKeys;
import DataObjects.PushEventKeys;
import DataObjects.SpecialMediaType;
import EventObjects.EventReport;
import EventObjects.EventType;

import static com.crashlytics.android.Crashlytics.log;
import static java.util.AbstractMap.SimpleEntry;


/**
 * <pre>
 * A Proxy that manages storage server operations.
 * Provided operations:
 * - Download file
 *
 * @author Mor
 */
public class StorageServerProxyService extends AbstractServerProxy {

    //region Service actions
    public static final String ACTION_DOWNLOAD              =   "com.services.StorageServerProxyService.DOWNLOAD";
    public static final String ACTION_NOTIFY_MEDIA_CLEARED  =   "com.services.StorageServerProxyService.NOTIFY_MEDIA_CLEARED";
    public static final String ACTION_CLEAR_MEDIA           =   "com.services.StorageServerProxyService.CLEAR_MEDIA";
    //endregion

    //region Service intent keys
    public static final String DESTINATION_ID               =   "DESTINATION_ID";
    public static final String SPECIAL_MEDIA_TYPE           =   "SPECIAL_MEDIA_TYPE";
    public static final String TRANSFER_DETAILS             =   "TRANSFER_DETAILS";
    //endregion

    //region Action URLs
    protected static final String URL_DOWNLOAD = ROOT_URL + "/v1/DownloadFile";
    //endregion

    private CollectionsUtils<DataKeys,Object> collectionsUtils = new CollectionsUtils<>();

    public StorageServerProxyService() {
        super(StorageServerProxyService.class.getSimpleName());
    }

    //region Service methods
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        log(Log.INFO,TAG, "Started");

        boolean shouldStop = handleCrashedService(flags, startId);
        if (shouldStop)
            return START_REDELIVER_INTENT;

        final Intent intentForThread = intent;

        new Thread() {
            @Override
            public void run() {

                if (intentForThread != null) {
                    String action = intentForThread.getAction();
                    log(Log.INFO,TAG, "ClientActionType:" + action);

                    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

                    List<SimpleEntry> data = getDefaultMessageData();

                    try {
                        switch (action) {
                            case ACTION_DOWNLOAD:
                                actionDownload(intentForThread, powerManager, data);
                                break;

//                            case ACTION_CLEAR_MEDIA:
//                                actionClear(intentForThread, data);
//                                break;
//
//                            case ACTION_NOTIFY_MEDIA_CLEARED:
//                                actionNotifyMediaCleared(intentForThread, data);
//                                break;

                            default:
                                setMidAction(false);
                                log(Log.WARN,TAG, "Service started with invalid action:" + action);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        String errMsg = "ClientActionType failed:" + action + " Exception:" + e.getMessage();
                        handleActionFailed();
                        log(Log.ERROR,TAG, errMsg);
                    } catch (Exception e) {
                        e.printStackTrace();
                        String errMsg = "ClientActionType failed:" + action + " Exception:" + e.getMessage();
                        handleActionFailed();
                        log(Log.ERROR,TAG, errMsg);
                    }
                } else
                    log(Log.WARN,TAG, "Service started with missing action");
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

    //region ClientActionType methods
    private void actionDownload(Intent intent, PowerManager powerManager, List<SimpleEntry> data) throws IOException {

        setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + "_wakeLock");
        wakeLock.acquire();
        HashMap pushData = (HashMap) intent.getSerializableExtra(PushEventKeys.PUSH_DATA);
        collectionsUtils.addMapElementsToSimpleEntryList(data, pushData);
        data.add(new SimpleEntry<>(DataKeys.DESTINATION_LOCALE, Locale.getDefault().getLanguage()));
        requestDownloadFromServer(openSocket(null), data);
    }

//    private void actionClear(Intent intent, HashMap<DataKeys,Object> data) throws IOException {
//
//        setMidAction(true);
//        ConnectionToServer connectionToServer = openSocket();
//        String destId = intent.getStringExtra(DESTINATION_ID);
//        SpecialMediaType specialMediaType = (SpecialMediaType) intent.getSerializableExtra(SPECIAL_MEDIA_TYPE);
//        data.put(DataKeys.SOURCE_LOCALE, Locale.getDefault().getLanguage());
//        sendClearCommandToServer(connectionToServer, destId, specialMediaType, data);
//    }

//    private void actionNotifyMediaCleared(Intent intent, HashMap<DataKeys,Object> data) throws IOException {
//
//        setMidAction(true);
//        ConnectionToServer connectionToServer = openSocket();
//        HashMap tdData = (HashMap) intent.getSerializableExtra(TRANSFER_DETAILS);
//        tdData.putAll(data);
//        tdData.put(DataKeys.DESTINATION_LOCALE, Locale.getDefault().getLanguage());
//        MessageToServer msgNMC = new MessageToServer(ServerActionType.NOTIFY_MEDIA_CLEARED, Constants.MY_ID(getApplicationContext()), tdData);
//        connectionToServer.sendToServer(msgNMC);
//    }

    private void handleActionFailed() {

        BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.STORAGE_ACTION_FAILURE));
    }
    //endregion

    //region Storage operations methods

//    /**
//     * Sends a clear media command to the server
//     *
//     * @param connectionToServer
//     * @param destId             The destination id of the user for which the media will be cleared
//     * @param specialMediaType   The special media type of the media to clear
//     * @throws IOException
//     */
//    private void sendClearCommandToServer(
//            ConnectionToServer connectionToServer,
//            String destId,
//            SpecialMediaType specialMediaType,
//            HashMap<DataKeys,Object> data) throws IOException {
//
//        data.put(DataKeys.DESTINATION_ID, destId);
//        data.put(DataKeys.SPECIAL_MEDIA_TYPE, specialMediaType);
//        data.put(DataKeys.DESTINATION_CONTACT_NAME, ContactsUtils.getContactName(this, destId));
//        data.put(DataKeys.SOURCE_ID, Constants.MY_ID(this));
//
//        MessageToServer msgCM = new MessageToServer(ServerActionType.CLEAR_MEDIA, Constants.MY_ID(this), data);
//        connectionToServer.sendToServer(msgCM);
//    }

    /**
     * Requests a download from the server
     *
     * @param connectionToServer The connection to the server
     * @param data The transfer details data
     */
    private void requestDownloadFromServer(ConnectionToServer connectionToServer, List<SimpleEntry> data) throws IOException {

        String sourceId = collectionsUtils.extractValueFromSimpleEntryList(DataKeys.SOURCE_ID, data).toString();
        String fileName = collectionsUtils.extractValueFromSimpleEntryList(DataKeys.SOURCE_WITH_EXTENSION,data).toString();

        File folderPath;

        switch(SpecialMediaType.valueOf(collectionsUtils.extractValueFromSimpleEntryList(DataKeys.SPECIAL_MEDIA_TYPE, data).toString()))
        {
            case CALLER_MEDIA:
                folderPath = new File(Constants.INCOMING_FOLDER + sourceId);
                break;
            case PROFILE_MEDIA:
                folderPath = new File(Constants.OUTGOING_FOLDER + sourceId);
                break;
            default:
                throw new UnsupportedOperationException("Not yet implemented");

        }

        long fileSize = Long.valueOf(collectionsUtils.extractValueFromSimpleEntryList(DataKeys.FILE_SIZE, data).toString());
        connectionToServer.download(URL_DOWNLOAD, folderPath.getAbsolutePath(), fileName, fileSize, data);
    }

    //endregion
}