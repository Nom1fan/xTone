package com.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.actions.ClientAction;
import com.actions.ActionFactory;
import com.data_objects.Constants;
import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;

import java.io.IOException;
import java.util.ArrayList;

import ClientObjects.ConnectionToServer;
import ClientObjects.IServerProxy;
import DataObjects.SharedConstants;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.MessageToClient;

/**
 * Created by mor on 18/10/2015.
 */
public abstract class AbstractServerProxy extends Service implements IServerProxy {

    public static final String ACTION_RECONNECT = "com.services.LogicServerProxyService.RECONNECT";
    public static final String ACTION_RESET_RECONNECT_INTERVAL = "com.services.LogicServerProxyService.RESET_RECONNECT_INTERVAL";
    protected static final long INITIAL_RETRY_INTERVAL = 1000 * 1;
    protected static final long MAXIMUM_RETRY_INTERVAL = 1000 * 10;
    protected String TAG;
    protected PowerManager.WakeLock wakeLock;
    protected ConnectivityManager connManager;
    protected ArrayList<ConnectionToServer> connections = new ArrayList<>();

    public AbstractServerProxy(String tag) {
        TAG = tag;
    }

    //region Service methods
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        SharedConstants.INCOMING_FOLDER = Constants.INCOMING_FOLDER;

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {

        connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        Log.i(TAG, "Created");
        //callInfoToast(TAG + " created");
    }

    @Override
    public void onDestroy() {

        Log.e(TAG, "Being destroyed");
        //callErrToast(TAG + "is being destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //endregion

    //region IServerProxy methods
    @Override
    public void handleDisconnection(String errMsg) {

        Log.e(TAG, errMsg);
        //BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.DISPLAY_ERROR, errMsg, null));
    }

    @Override
    public void handleMessageFromServer(MessageToClient msg, ConnectionToServer connectionToServer) {

        try {
            ClientAction clientAction = ActionFactory.instance().getAction(msg.getActionType());
            clientAction.setConnectionToServer(connectionToServer);
            EventReport eventReport = clientAction.doClientAction(msg.getData());

            if (eventReport.status() != EventType.NO_ACTION_REQUIRED)
                BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, eventReport);

            setMidAction(false);

        } catch (Exception e) {
            String errMsg = "Handling message from server failed. Reason:" + e.getMessage();
            Log.e(TAG, errMsg, e);
        } finally {
            try {
                // Finished handling request-response transaction
                connectionToServer.closeConnection();
            } catch(Exception ignored) { }
            connections.remove(connectionToServer);
            releaseLockIfNecessary();
        }
    }
    //endregion

    //region Internal operations methods
    protected ConnectionToServer openSocket(String host, int port) throws IOException {
        Log.i(TAG, "Opening socket...");
        ConnectionToServer connectionToServer = new ConnectionToServer(host, port, this);
        connectionToServer.openConnection();
        connections.add(connectionToServer);
        Log.i(TAG, "Socket is open");

        return connectionToServer;
    }

    protected void releaseLockIfNecessary() {

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    protected final void reconnect(String host, int port) throws IOException {

        Log.i(TAG, "Reconnecting...");
        ConnectionToServer connectionToServer = new ConnectionToServer(host, port, this);
        connectionToServer.openConnection();
        connectionToServer.closeConnection();
        Log.i(TAG, "Reconnected successfully");

    }

    protected void cancelReconnect() {
        Log.i(TAG, "Cancelling reconnect");
        Intent i = new Intent();
        i.setClass(this, LogicServerProxyService.class);
        i.setAction(ACTION_RECONNECT);
        PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.cancel(pi);
    }

    protected void scheduleReconnect(long startTime) {
        Log.i(TAG, "Scheduling reconnect");
        long interval =
                SharedPrefUtils.getLong(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.RECONNECT_INTERVAL, INITIAL_RETRY_INTERVAL);

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        if (elapsed < interval)
            interval = Math.min(interval * 2, MAXIMUM_RETRY_INTERVAL);
        else
            interval = INITIAL_RETRY_INTERVAL;

        Log.i(TAG, "Rescheduling connection in " + interval + "ms.");

        SharedPrefUtils.setLong(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.RECONNECT_INTERVAL, interval);


        Intent i = new Intent();
        i.setClass(this, LogicServerProxyService.class);
        i.setAction(ACTION_RECONNECT);
        PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, now + interval, pi);
    }

    protected boolean wasMidAction() {

        return SharedPrefUtils.getBoolean(this, SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.WAS_MID_ACTION);
    }

    protected boolean handleCrashedService(int flags, int startId) {

        boolean shouldStop = false;
        // If crash restart occurred but was not mid-action we should do nothing
        if ((flags & START_FLAG_REDELIVERY) != 0 && !wasMidAction()) {
            Log.i(TAG, "Crash restart occurred but was not mid-action (wasMidAction()=" + wasMidAction() + ". Exiting service.");
            stopSelf(startId);
            shouldStop = true;
        }

        return shouldStop;
    }

    protected void markCrashedServiceHandlingComplete(int flags, int startId) {

        if ((flags & START_FLAG_REDELIVERY) != 0) { // if we took care of crash restart, mark it as completed
            stopSelf(startId);
        }
    }

    protected void setMidAction(boolean bool) {

        Log.i(TAG, "Setting midAction=" + bool);
        SharedPrefUtils.setBoolean(this, SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.WAS_MID_ACTION, bool);
    }

    protected boolean isNetworkAvailable() {

        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean wifiConnected = (wifiInfo != null && wifiInfo.isConnected());

        NetworkInfo mobileInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean mobileConnected = (mobileInfo != null && mobileInfo.isConnected());

        return wifiConnected || mobileConnected;
    }
    //endregion
}
