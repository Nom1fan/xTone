package com.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.services.LogicServerProxyService;

import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;

import com.app.AppStateManager;

/**
 * Created by mor on 01/10/2015.
 */
public class ConnectivityBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = ConnectivityBroadcastReceiver.class.getSimpleName();
    private Context _context;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        _context = context;

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean wifiConnected = (wifiInfo != null && wifiInfo.isConnected());

        NetworkInfo mobileInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean mobileConnected = (mobileInfo != null && mobileInfo.isConnected());

        Log.i(TAG, "Connectivity changed. Wifi=" + wifiConnected + ". Mobile=" + mobileConnected);

        if (wifiConnected || mobileConnected) {

            String appState = AppStateManager.getAppState(context);
            Log.i(TAG, "App State:"+appState);
            if(!appState.equals(AppStateManager.STATE_LOGGED_OUT) && appState.equals(AppStateManager.STATE_DISABLED)) {
                Log.i(TAG, "Starting LogicServerProxyService...");
                Intent i = new Intent(context, LogicServerProxyService.class);
                i.setAction(LogicServerProxyService.ACTION_RESET_RECONNECT_INTERVAL);
                context.startService(i);
                i.setAction(LogicServerProxyService.ACTION_RECONNECT);
                context.startService(i);
            }
        }
        else {
            sendEventReportBroadcast(new EventReport(EventType.DISCONNECTED, "Disconnected. Check your internet connection", null));
        }
    }

    private void sendEventReportBroadcast(EventReport report) {

        Log.i(TAG, "Broadcasting event:" + report.status().toString());
        Intent broadcastEvent = new Intent(Event.EVENT_ACTION);
        broadcastEvent.putExtra(Event.EVENT_REPORT, report);
        _context.sendBroadcast(broadcastEvent);
    }
}
