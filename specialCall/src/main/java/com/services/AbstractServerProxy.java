package com.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.actions.ActionFactory;
import com.actions.ClientAction;
import com.data_objects.Constants;
import com.utils.BroadcastUtils;
import com.utils.NetworkingUtils;
import com.utils.SharedPrefUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import ClientObjects.ConnectionToServer;
import ClientObjects.IServerProxy;
import DataObjects.DataKeys;
import DataObjects.SharedConstants;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.MessageToClient;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by mor on 18/10/2015.
 */
public abstract class AbstractServerProxy extends Service implements IServerProxy {

    //region Service intent keys
    public static final String ACTION_TO_CANCEL = "ACTION_TO_CANCEL";
    //endregion

    protected String host;
    protected int port;
    protected String TAG;
    protected PowerManager.WakeLock wakeLock;
    protected List<ConnectionToServer> connections = new LinkedList<>();

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
        connections.remove(cts);
        if(NetworkingUtils.isNetworkAvailable(getApplicationContext()))
            BroadcastUtils.sendEventReportBroadcast(this, TAG, new EventReport(EventType.LOADING_TIMEOUT));
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
    //endregion

    //region Internal operations methods
    protected ConnectionToServer openSocket() throws IOException {
        log(Log.INFO,TAG, "Opening socket...");
        ConnectionToServer connectionToServer = new ConnectionToServer(host, port, this);
        connectionToServer.openConnection();
        connections.add(connectionToServer);
        log(Log.INFO,TAG, "Socket is open");

        return connectionToServer;
    }

    protected void releaseLockIfNecessary() {

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    protected boolean wasMidAction() {

        return SharedPrefUtils.getBoolean(this, SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.WAS_MID_ACTION);
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

    protected void setMidAction(boolean bool) {

        log(Log.INFO,TAG, "Setting midAction=" + bool);
        SharedPrefUtils.setBoolean(this, SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.WAS_MID_ACTION, bool);
    }

    protected HashMap<DataKeys, Object> getDefaultMessageData() {
        HashMap<DataKeys, Object> data = new HashMap<>();
        data.put(DataKeys.APP_VERSION, Constants.APP_VERSION());
        return data;
    }
    //endregion
    //endregion
}
