package com.services;

import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.actions.ActionFactory;
import com.actions.ClientAction;
import com.client.ConnectionToServer;
import com.client.IServerProxy;
import com.data_objects.Constants;
import com.google.gson.reflect.TypeToken;
import com.utils.BroadcastUtils;
import com.utils.CollectionsUtils;
import com.utils.SharedPrefUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    protected CollectionsUtils<DataKeys,Object> collectionsUtils = new CollectionsUtils<>();

    //region Response types
    protected class ResponseTypes {

        public final Type TYPE_EVENT_REPORT = new TypeToken<MessageToClient<EventReport>>() {
        }.getType();
        public final Type TYPE_MAP = new TypeToken<MessageToClient<Map<DataKeys,Object>>>() {
        }.getType();
    }
    protected ResponseTypes responseTypes = new ResponseTypes();
    //endregion

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
            sendLoadingTimeout();

    }

    private void sendLoadingTimeout() {
        BroadcastUtils.sendEventReportBroadcast(this, TAG, new EventReport(EventType.LOADING_TIMEOUT));
    }

    //endregion
    @Override
    public void handleMessageFromServer(MessageToClient msg, ConnectionToServer connectionToServer) {
        ClientAction clientAction = null;
        try {
            clientAction = ActionFactory.instance().getAction(msg.getActionType());
            clientAction.setConnectionToServer(connectionToServer);
            EventReport eventReport = clientAction.doClientAction(msg.getResult());

            if(eventReport==null)
                log(Log.WARN, TAG, "ClientAction:" + clientAction.getClass().getSimpleName() + " returned null eventReport");
            else if (eventReport.status() != EventType.NO_ACTION_REQUIRED)
                BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, eventReport);

            setMidAction(false);

        } catch (Exception e) {
            String errMsg;
            if (clientAction == null)
                errMsg = "Handling message from server failed. ClientAction was null. Message:" + msg.getActionType();
            else
                errMsg = "Handling message from server failed. ClientAction:" + clientAction.getClass().getSimpleName() + " Reason:" + e.getMessage();
            log(Log.ERROR, TAG, errMsg);
            handleDisconnection(connectionToServer, errMsg);
        } finally {
            releaseLockIfNecessary();
        }
    }

    //region Internal operations methods
    protected ConnectionToServer openSocket(Type responseType) throws IOException {
        ConnectionToServer connectionToServer = new ConnectionToServer(this, responseType);
        connections.add(connectionToServer);
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

    protected boolean isNetworkAvailable() {

        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    protected List<SimpleEntry> getDefaultMessageData() {
        List<SimpleEntry> data = new LinkedList<>();
        data.add(new SimpleEntry<>(DataKeys.MESSAGE_INITIATER_ID, Constants.MY_ID(this)));
        data.add(new SimpleEntry<>(DataKeys.APP_VERSION, Constants.APP_VERSION()));
        return data;
    }

    //endregion
    //endregion
}
