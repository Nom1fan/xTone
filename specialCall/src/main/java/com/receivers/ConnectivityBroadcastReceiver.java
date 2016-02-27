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
import com.utils.BroadcastUtils;

/**
 * Created by mor on 01/10/2015.
 */
public class ConnectivityBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = ConnectivityBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean wifiConnected = (wifiInfo != null && wifiInfo.isConnected());

        NetworkInfo mobileInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean mobileConnected = (mobileInfo != null && mobileInfo.isConnected());

        Log.i(TAG, "Connectivity changed. Wifi=" + wifiConnected + ". Mobile=" + mobileConnected);

        if (wifiConnected || mobileConnected) {

            String appState = AppStateManager.getAppState(context);
            Log.i(TAG, "App State:" + appState);
            if (!appState.equals(AppStateManager.STATE_LOGGED_OUT) && appState.equals(AppStateManager.STATE_DISABLED)) {
                Log.i(TAG, "Starting LogicServerProxyService...");
                Intent i = new Intent(context, LogicServerProxyService.class);
                i.setAction(LogicServerProxyService.ACTION_RESET_RECONNECT_INTERVAL);
                context.startService(i);
                i.setAction(LogicServerProxyService.ACTION_RECONNECT);
                context.startService(i);
            }
        } else {
            BroadcastUtils.sendEventReportBroadcast(context, TAG,
                    new EventReport(EventType.DISCONNECTED,
                            "Disconnected. Check your internet connection", null));
        }
    }

}
