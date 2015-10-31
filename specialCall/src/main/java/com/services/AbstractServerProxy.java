package com.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

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
public class AbstractServerProxy extends Service implements IServerProxy {

    protected Context mContext;
    protected String TAG;
    protected PowerManager.WakeLock wakeLock;
    protected ConnectivityManager connManager;
    protected ArrayList<ConnectionToServer> connections = new ArrayList<>();

    public static final String ACTION_RECONNECT = "com.services.LogicServerProxyService.RECONNECT";

    protected static final long INITIAL_RETRY_INTERVAL = 1000 * 5;
    protected static final long MAXIMUM_RETRY_INTERVAL = 1000 * 60;

    public AbstractServerProxy(String tag) {
        TAG = tag;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mContext = getApplicationContext();
        SharedConstants.MY_ID = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER);
        SharedConstants.DEVICE_TOKEN = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN);
        SharedConstants.specialCallPath = Constants.specialCallPath;

        return START_NOT_STICKY;
    }

    @Override
    public void handleMessageFromServer(MessageToClient msg, ConnectionToServer connectionToServer) {
        try
        {
            EventReport eventReport = msg
                    .doClientAction(connectionToServer);

            if(eventReport.status()!= EventType.NO_ACTION_REQUIRED)
                BroadcastUtils.sendEventReportBroadcast(mContext,TAG, eventReport);

            releaseLockIfNecessary();

            // Finished handling request-response transaction
            connectionToServer.closeConnection();
            connections.remove(connectionToServer);

        } catch(Exception e) {
            String errMsg = "Handling message from server failed. Reason:"+e.getMessage();
            Log.i(TAG, errMsg);
            releaseLockIfNecessary();
            //handleDisconnection(errMsg);
        }
    }

    protected ConnectionToServer openSocket(String host, int port) throws IOException {
        Log.i(TAG, "Opening socket...");
        ConnectionToServer connectionToServer = new ConnectionToServer(host, port, this);
        connectionToServer.openConnection();
        connections.add(connectionToServer);
        Log.i(TAG, "Socket is open");

        return connectionToServer;
    }

    @Override
    public void onCreate() {

        connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        Log.i(TAG, "created");
        callInfoToast(TAG + " created");
    }

    @Override
    public void onDestroy() {

        Log.e(TAG, "Being destroyed");
        callErrToast(TAG + "is being destroyed");
    }

    @Override
    public void handleDisconnection(String errMsg) {

        Log.e(TAG, errMsg);

        BroadcastUtils.sendEventReportBroadcast(mContext, TAG, new EventReport(EventType.DISPLAY_ERROR, errMsg, null));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* Internal operations methods */

    protected void releaseLockIfNecessary() {

        if(wakeLock!=null) {
            wakeLock.release();
            wakeLock = null;
        }
    }


    protected void cancelReconnect()
    {
        Log.i(TAG, "Cancelling reconnect");
        Intent i = new Intent();
        i.setClass(this, LogicServerProxyService.class);
        i.setAction(ACTION_RECONNECT);
        PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmMgr.cancel(pi);
    }

    protected void scheduleReconnect(long startTime)
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

    protected boolean isNetworkAvailable() {

        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean wifiConnected = (wifiInfo != null && wifiInfo.isConnected());

        NetworkInfo mobileInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean mobileConnected = (mobileInfo != null && mobileInfo.isConnected());

        return wifiConnected || mobileConnected;
    }

              /* UI methods */

    protected void callErrToast(final String text) {

        Toast toast = Toast.makeText(getApplicationContext(), text,
                Toast.LENGTH_LONG);
        TextView v = (TextView) toast.getView().findViewById(
                android.R.id.message);
        v.setTextColor(Color.RED);
        toast.show();
    }

    protected void callInfoToast(final String text) {

        Toast toast = Toast.makeText(getApplicationContext(), text,
                Toast.LENGTH_LONG);
        TextView v = (TextView) toast.getView().findViewById(
                android.R.id.message);
        v.setTextColor(Color.GREEN);
        toast.show();
    }

    protected void callInfoToast(final String text, final int g) {

        Toast toast = Toast.makeText(getApplicationContext(), text,
                Toast.LENGTH_LONG);
        TextView v = (TextView) toast.getView().findViewById(
                android.R.id.message);
        v.setTextColor(g);
        toast.show();
    }
}
