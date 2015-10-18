package com.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.async_tasks.UploadTask;
import com.data_objects.Constants;
import com.special.app.R;
import com.utils.AppStateUtils;
import com.utils.SharedPrefUtils;
import java.io.IOException;
import ClientObjects.ConnectionToServer;
import ClientObjects.IServerProxy;
import DataObjects.PushEventKeys;
import DataObjects.SharedConstants;
import DataObjects.TransferDetails;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;
import MessagesToClient.MessageToClient;
import MessagesToServer.MessageRequestDownload;


/**
 * <pre>
 * A Proxy that manages storage server operations.
 * Provided operations:
 * - Download file
 * - Upload file
 * @author Mor
 */
public class StorageServerProxyService extends Service implements IServerProxy {

    // Service actions
    public static final String ACTION_RECONNECT = "com.services.StorageServerProxyService.RECONNECT";
    public static final String ACTION_DOWNLOAD = "com.services.StorageServerProxyService.DOWNLOAD";
    public static final String ACTION_UPLOAD = "com.services.StorageServerProxyService.UPLOAD";

    // Service intent keys
    public static final String FILE_TO_UPLOAD = "com.services.StorageServerProxyService.FILE_TO_UPLOAD";
    public static final String DESTINATION_ID = "com.services.StorageServerProxyService.DESTINATION_ID";

    private ConnectionToServer connectionToServer;
    private ConnectivityManager connManager;
    private WakeLock wakeLock;
    private static final long INITIAL_RETRY_INTERVAL = 1000 * 5;
    private static final long MAXIMUM_RETRY_INTERVAL = 1000 * 60;
    private static final int ONGOING_NOTIFICATION_ID = 1337;
    private static final String TAG = StorageServerProxyService.class.getSimpleName();

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
                        connectIfNecessary();

                        switch (action) {

                            case ACTION_DOWNLOAD:
                                startServiceForeground();
                                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + "_wakeLock");
                                wakeLock.acquire();
                                TransferDetails td = (TransferDetails) intentForThread.getSerializableExtra(PushEventKeys.PUSH_DATA);
                                requestDownloadFromServer(td);
                            break;

                            case ACTION_UPLOAD: {
                                startServiceForeground();
                                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + "_wakeLock");
                                wakeLock.acquire();
                                String destId = intentForThread.getStringExtra(DESTINATION_ID);
                                FileManager managedFile = (FileManager) intentForThread.getSerializableExtra(FILE_TO_UPLOAD);
                                uploadFileToServer(destId, managedFile);
                                releaseLockIfNecessary();
                                stopForeground(true);
                            }
                            break;

                            default:
                                Log.w(TAG, "Service started with action:" + action);

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
    public void onCreate() {

        Log.i(TAG, "StorageServerProxyService created");
        callInfoToast("StorageServerProxyService created");
        SharedConstants.MY_ID = SharedPrefUtils.getString(getApplicationContext(),SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER);
        SharedConstants.DEVICE_TOKEN = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN);
        SharedConstants.specialCallPath = Constants.specialCallPath;

        connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    }

    @Override
    public void onDestroy() {

        Log.e(TAG, "StorageServerProxyService is being destroyed");
        callErrToast("StorageServerProxyService is being destroyed");
        if(connectionToServer!=null)
        {
            try {
                connectionToServer.closeConnection();
            }
            catch (IOException ignored) {}
            connectionToServer = null;
        }
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
    public void uploadFileToServer(final String destNumber, final FileManager managedFile) throws IOException {

        TransferDetails td = new TransferDetails(SharedConstants.MY_ID, destNumber, managedFile);
        new UploadTask(getApplicationContext(), connectionToServer, td).execute();
    }

    /**
     * Requests a download from the server
     * @param td - The transfer details
     */
    public void requestDownloadFromServer(TransferDetails td) throws IOException {

        MessageRequestDownload msgRD = new MessageRequestDownload(td);
        connectionToServer.sendToServer(msgRD);

    }


    /**
     *
     * @param msg - The response message to handle
     */
    @Override
    public void handleMessageFromServer(MessageToClient msg) {

        try
        {
            EventReport eventReport = msg
                    .doClientAction(this);

            if(eventReport.status()!=EventType.NO_ACTION_REQUIRED)
                sendEventReportBroadcast(eventReport);

            releaseLockIfNecessary();
            stopForeground(true);

        } catch(Exception e) {
            String errMsg = "ClientAction failed. Reason:"+e.getMessage();
            Log.i(TAG, errMsg);
            releaseLockIfNecessary();
            stopForeground(true);
            //handleDisconnection(errMsg);
        }
    }

    public ConnectionToServer getConnectionToServer() {
        return connectionToServer;
    }

          /* Internal server operations methods */

    private void openSocket() throws IOException {
        Log.i(TAG, "Opening socket...");
        //sendEventReportBroadcast(new EventReport(EventType.CONNECTING, "Opening data port...", null));
        connectionToServer = new ConnectionToServer(SharedConstants.STROAGE_SERVER_HOST, SharedConstants.STORAGE_SERVER_PORT, this);
        connectionToServer.openConnection();
        //sendEventReportBroadcast(new EventReport(EventType.CONNECTED, "Connected", null));
        Log.i(TAG, "Socket is open");
    }

    /**
     * Connects to server if necessary
     * @throws IOException
     */
    private synchronized void connectIfNecessary() throws IOException {

        if (isNetworkAvailable())
        {
            if(connectionToServer==null || !connectionToServer.isConnected())
                openSocket();
            else {
                cancelReconnect();
                SharedPrefUtils.setLong(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.RECONNECT_INTERVAL, INITIAL_RETRY_INTERVAL);
            }
        }
        else {
            scheduleReconnect(System.currentTimeMillis());
            if(!AppStateUtils.getAppState(getApplicationContext()).equals(AppStateUtils.STATE_DISABLED))
                sendEventReportBroadcast(new EventReport(EventType.DISCONNECTED, "Disconnected. Check your internet connection", null));
        }
    }

    private void sendEventReportBroadcast(EventReport report) {

        Log.i(TAG, "Broadcasting event:" + report.status().toString());
        Intent broadcastEvent = new Intent(Event.EVENT_ACTION);
        broadcastEvent.putExtra(Event.EVENT_REPORT, report);
        sendBroadcast(broadcastEvent);
    }

    private boolean isNetworkAvailable() {

        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean wifiConnected = (wifiInfo != null && wifiInfo.isConnected());

        NetworkInfo mobileInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean mobileConnected = (mobileInfo != null && mobileInfo.isConnected());

        return wifiConnected || mobileConnected;
    }

    /**
     * Deals with disconnection and schedules a reconnect
     * This method is called by ConnectionToServer connectionException() method
     * @param errMsg
     */
    public void handleDisconnection(String errMsg) {

        Log.e(TAG, errMsg);
        if(connectionToServer!=null) {
            try {
                connectionToServer.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connectionToServer = null;
            sendEventReportBroadcast(new EventReport(EventType.DISCONNECTED, errMsg, null));
            scheduleReconnect(System.currentTimeMillis());
        }
    }

    /* Internal operations methods */
    private void releaseLockIfNecessary() {

        if(wakeLock!=null) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    private void cancelReconnect()
    {
        Log.i(TAG, "Cancelling reconnect");
        Intent i = new Intent();
        i.setClass(this, StorageServerProxyService.class);
        i.setAction(ACTION_RECONNECT);
        PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmMgr.cancel(pi);
    }

    private void scheduleReconnect(long startTime)
    {
        Log.i(TAG, "Scheduling reconnect");
        long interval =
                SharedPrefUtils.getLong(getApplicationContext(),SharedPrefUtils.SERVER_PROXY,SharedPrefUtils.RECONNECT_INTERVAL, INITIAL_RETRY_INTERVAL);

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        if (elapsed < interval)
            interval = Math.min(interval * 2, MAXIMUM_RETRY_INTERVAL);
        else
            interval = INITIAL_RETRY_INTERVAL;

        Log.i(TAG, "Rescheduling connection in " + interval + "ms.");

        SharedPrefUtils.setLong(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.RECONNECT_INTERVAL, interval);


        Intent i = new Intent();
        i.setClass(this, StorageServerProxyService.class);
        i.setAction(ACTION_RECONNECT);
        PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, now + interval, pi);
    }

    private void startServiceForeground() {

        Notification notification = new Notification(android.R.drawable.stat_sys_upload, getText(R.string.upload_ticker_text),
                System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, StorageServerProxyService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, getText(R.string.notification_title),
                getText(R.string.notification_message), pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

          /* UI methods */

    private void callErrToast(final String text) {

        Toast toast = Toast.makeText(getApplicationContext(), text,
                Toast.LENGTH_LONG);
        TextView v = (TextView) toast.getView().findViewById(
                android.R.id.message);
        v.setTextColor(Color.RED);
        toast.show();
    }

    private void callInfoToast(final String text) {

        Toast toast = Toast.makeText(getApplicationContext(), text,
                Toast.LENGTH_LONG);
        TextView v = (TextView) toast.getView().findViewById(
                android.R.id.message);
        v.setTextColor(Color.GREEN);
        toast.show();
    }

    private void callInfoToast(final String text, final int g) {

        Toast toast = Toast.makeText(getApplicationContext(), text,
                Toast.LENGTH_LONG);
        TextView v = (TextView) toast.getView().findViewById(
                android.R.id.message);
        v.setTextColor(g);
        toast.show();
    }
}