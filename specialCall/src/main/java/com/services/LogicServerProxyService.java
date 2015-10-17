package com.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.data_objects.Constants;
import com.utils.AppStateUtils;
import com.utils.SharedPrefUtils;

import java.io.IOException;

import ClientObjects.ConnectionToServer;
import ClientObjects.IServerProxy;
import DataObjects.SharedConstants;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.MessageToClient;
import MessagesToServer.MessageIsRegistered;
import MessagesToServer.MessageRegister;


/**
 * <pre>
 * A Proxy that manages logic server operations.
 * Provided operations:
 * - Register
 * - Check if user is registered
 * @author Mor
 */
public class LogicServerProxyService extends Service implements IServerProxy {

    // Service actions
    public static final String ACTION_REGISTER = "com.services.LogicServerProxyService.REGISTER";
    public static final String ACTION_RECONNECT = "com.services.LogicServerProxyService.RECONNECT";
    public static final String ACTION_ISREGISTERED = "com.services.LogicServerProxyService.ISREGISTERED";

    // Service intent keys
    public static final String DESTINATION_ID = "com.services.LogicServerProxyService.DESTINATION_ID";

    private ConnectionToServer connectionToServer;
    private ConnectivityManager connManager;
    private static final long INITIAL_RETRY_INTERVAL = 1000 * 5;
    private static final long MAXIMUM_RETRY_INTERVAL = 1000 * 60;
    private static final String TAG = LogicServerProxyService.class.getSimpleName();

    /* Service overriding methods */

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "LogicServerProxyService started");

        final Intent intentForThread = intent;

        new Thread() {

            @Override
            public void run() {
                if (intentForThread != null)
                {
                    String action = intentForThread.getAction();
                    Log.i(TAG, "Action:" + action);

                    try {
                        connectIfNecessary();

                        switch (action) {

                            case ACTION_REGISTER:
                                register();
                            break;

                            case ACTION_ISREGISTERED: {
                                String destId = intentForThread.getStringExtra(DESTINATION_ID);
                                isRegistered(destId);
                            }
                            break;

                            default:
                                Log.w(TAG, "Service started with action:" + action);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        String errMsg = "Action failed:"+action+" Exception:"+e.getMessage();
                        Log.e(TAG, errMsg);
                        //handleDisconnection(errMsg);
                    }
                } else
                    Log.w(TAG, "Service started with missing action");


            }
        }.start();

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {

        Log.i(TAG, "LogicServerProxyService created");
        callInfoToast("LogicServerProxyService created");
        SharedConstants.MY_ID = SharedPrefUtils.getString(getApplicationContext(),SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER);
        SharedConstants.DEVICE_TOKEN = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN);
        SharedConstants.specialCallPath = Constants.specialCallPath;

        connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    }

    @Override
    public void onDestroy() {

        Log.e(TAG, "LogicServerProxyService is being destroyed");
        callErrToast("LogicServerProxyService is being destroyed");
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

        } catch(Exception e) {
            String errMsg = "ClientAction failed. Reason:"+e.getMessage();
            Log.i(TAG, errMsg);
            //handleDisconnection(errMsg);
        }
    }


    /**
     * Enables to check if a destination number is logged-in/online
     *
     * @param destinationId - The number of whom to check is logged-in
     */
    public void isRegistered(final String destinationId)  throws IOException {
        MessageIsRegistered msgIsLogin = new MessageIsRegistered(SharedConstants.MY_ID, destinationId);
        connectionToServer.sendToServer(msgIsLogin);
    }

    public ConnectionToServer getConnectionToServer() {
        return connectionToServer;
    }

          /* Internal server operations methods */

    /**
     * Enables to register to the server
     */
    private void register() throws IOException {

        Log.i(TAG, "Initiating register sequence...");
        MessageRegister msgRegister = new MessageRegister(SharedConstants.MY_ID, SharedConstants.DEVICE_TOKEN);
        Log.i(TAG, "Sending register message to server...");
        connectionToServer.sendToServer(msgRegister);
    }

    private void openSocket() throws IOException {
        Log.i(TAG, "Opening socket...");
        sendEventReportBroadcast(new EventReport(EventType.CONNECTING, "Opening logic port...", null));
        connectionToServer = new ConnectionToServer(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT, this);
        connectionToServer.openConnection();
        sendEventReportBroadcast(new EventReport(EventType.CONNECTED, "Connected", null));
        Log.i(TAG, "Socket is open");
    }

    private synchronized void connectIfNecessary() throws IOException {

        if (isNetworkAvailable())
        {
            if(connectionToServer==null || !connectionToServer.isConnected()) {
                openSocket();
                register();
            }
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
        }
        sendEventReportBroadcast(new EventReport(EventType.DISCONNECTED, errMsg, null));
        scheduleReconnect(System.currentTimeMillis());
    }

    /* Internal operations methods */

    private void cancelReconnect()
    {
        Log.i(TAG, "Cancelling reconnect");
        Intent i = new Intent();
        i.setClass(this, LogicServerProxyService.class);
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
        i.setClass(this, LogicServerProxyService.class);
        i.setAction(ACTION_RECONNECT);
        PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, now + interval, pi);
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