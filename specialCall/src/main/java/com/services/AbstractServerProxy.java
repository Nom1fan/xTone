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

import com.actions.ActionFactory;
import com.actions.ClientAction;
import com.app.AppStateManager;
import com.data_objects.Constants;
import com.google.gson.reflect.TypeToken;
import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ClientObjects.ConnectionToServer;
import ClientObjects.IServerProxy;
import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.MessageToClient;

import static com.crashlytics.android.Crashlytics.log;
import static java.util.AbstractMap.SimpleEntry;

/**
 * Created by mor on 18/10/2015.
 */
public abstract class AbstractServerProxy extends Service implements IServerProxy {

    protected static final String HOST = Constants.SERVER_HOST;
    protected static final int PORT = Constants.SERVER_PORT;
    protected static final String ROOT_URL = "http://" + HOST + ":" + PORT;

    //region Response type tokens
    protected TypeToken<MessageToClient<EventReport>> RESPONSE_EVENT_REPORT = new TypeToken<MessageToClient<EventReport>>(){};
    protected TypeToken<MessageToClient<Map>> RESPONSE_MAP = new TypeToken<MessageToClient<Map>>(){};
    //endregion

    //region Service actions
    public static final String ACTION_RECONNECT = "com.services.LogicServerProxyService.RECONNECT";
    public static final String ACTION_RESET_RECONNECT_INTERVAL = "com.services.LogicServerProxyService.RESET_RECONNECT_INTERVAL";
    //endregion

    protected static final long INITIAL_RETRY_INTERVAL = 1000 * 1;
    protected static final long MAXIMUM_RETRY_INTERVAL = 1000 * 10;
    protected String TAG;
    protected PowerManager.WakeLock wakeLock;
    protected ConnectivityManager connManager;
    protected List<ConnectionToServer> connections = new LinkedList<>();

    public AbstractServerProxy(String tag) {
        TAG = tag;
    }

    //region Service methods
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {

        connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        log(Log.INFO,TAG, "Created");
        //callInfoToast(TAG + " created");
    }

    @Override
    public void onDestroy() {

        log(Log.ERROR,TAG, "Being destroyed");
        //callErrToast(TAG + "is being destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //endregion

    //region IServerProxy methods
    @Override
    public void handleDisconnection(ConnectionToServer cts, String errMsg) {

        log(Log.ERROR,TAG, errMsg);
        cts.closeConnection();
        connections.remove(cts);
        if(isNetworkAvailable())
            BroadcastUtils.sendEventReportBroadcast(this, TAG, new EventReport(EventType.LOADING_TIMEOUT));

    }

    //endregion
    @Override
    public void handleMessageFromServer(MessageToClient msg, ConnectionToServer connectionToServer) {

        try {
            ClientAction clientAction = ActionFactory.instance().getAction(msg.getActionType());
            clientAction.setConnectionToServer(connectionToServer);
            EventReport eventReport = clientAction.doClientAction(msg.getResult());

            if(eventReport==null)
                log(Log.WARN, TAG, "ClientAction:" + clientAction.getClass().getSimpleName() + " returned null eventReport");
            else if (eventReport.status() != EventType.NO_ACTION_REQUIRED)
                BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, eventReport);

            setMidAction(false);

        } catch (Exception e) {
            String errMsg = "Handling message from server failed. Reason:" + e.getMessage();
            log(Log.ERROR,TAG, errMsg);
        } finally {
            try {
                // Finished handling request-response transaction
                connectionToServer.closeConnection();
            } catch (Exception ignored) {
            }
            connections.remove(connectionToServer);
            releaseLockIfNecessary();
        }
    }

    //region Internal operations methods
    protected ConnectionToServer openSocket() throws IOException {
        ConnectionToServer connectionToServer = new ConnectionToServer(this);
        connections.add(connectionToServer);
        return connectionToServer;
    }

    protected void releaseLockIfNecessary() {

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    protected final boolean pingReceived() throws IOException {
        boolean pingOK;
        log(Log.INFO,TAG, "Pinging...");
        ConnectionToServer connectionToServer = new ConnectionToServer(this);
        pingOK = connectionToServer.ping(HOST, PORT);
        log(Log.INFO,TAG, "Ping successfull");
        return pingOK;
    }

    protected void cancelReconnect() {
        log(Log.INFO,TAG, "Cancelling reconnect");
        Intent i = new Intent();
        i.setClass(this, LogicServerProxyService.class);
        i.setAction(ACTION_RECONNECT);
        PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.cancel(pi);
    }

    protected void scheduleReconnect(long startTime) {

        log(Log.INFO,TAG, "Scheduling pingReceived");
        if(!isNetworkAvailable()) {
            if (!AppStateManager.getAppState(this).equals(AppStateManager.STATE_DISABLED))
                BroadcastUtils.sendEventReportBroadcast(this, TAG, new EventReport(EventType.DISCONNECTED));
        }

        long interval =
                SharedPrefUtils.getLong(this, SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.RECONNECT_INTERVAL, INITIAL_RETRY_INTERVAL);

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        if (elapsed < interval)
            interval = Math.min(interval * 2, MAXIMUM_RETRY_INTERVAL);
        else
            interval = INITIAL_RETRY_INTERVAL;

        log(Log.INFO,TAG, "Rescheduling connection in " + interval + "ms.");

        SharedPrefUtils.setLong(this, SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.RECONNECT_INTERVAL, interval);


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

    protected boolean isReconnecting() {
        return SharedPrefUtils.getBoolean(this, SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.IS_RECONNECTING);
    }

    protected boolean handleCrashedService(int flags, int startId) {

        boolean shouldStop = false;
        // If crash restart occurred but was not mid-action we should do nothing
        if ((flags & START_FLAG_REDELIVERY) != 0 && !wasMidAction()) {
            log(Log.INFO,TAG, "Crash restart occurred but was not mid-action (wasMidAction()=" + wasMidAction() + ". Exiting service.");
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

    protected void setReconnecting(boolean b) {

        SharedPrefUtils.setBoolean(this, SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.IS_RECONNECTING, b);
    }

    protected void setMidAction(boolean bool) {

        log(Log.INFO,TAG, "Setting midAction=" + bool);
        SharedPrefUtils.setBoolean(this, SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.WAS_MID_ACTION, bool);
    }

    protected boolean isNetworkAvailable() {

        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    protected List<SimpleEntry> getDefaultMessageData() {
        List<SimpleEntry> data = new LinkedList<>();
        data.add(new SimpleEntry<>(DataKeys.MESSAGE_INITIATER_ID, Constants.MY_ID(this)));
        data.add(new SimpleEntry<>(DataKeys.APP_VERSION.toString(), Constants.APP_VERSION()));
        return data;
    }

    //endregion
    //endregion
}
