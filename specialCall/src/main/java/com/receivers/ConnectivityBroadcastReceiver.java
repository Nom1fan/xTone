package com.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.android.services.ServerProxyService;

import data_objects.SharedPrefUtils;
import utils.AppStateUtils;

/**
 * Created by mor on 01/10/2015.
 */
public class ConnectivityBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = ConnectivityBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent)
    {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean wifiConnected = (wifiInfo != null && wifiInfo.isConnected());

        NetworkInfo mobileInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean mobileConnected = (mobileInfo != null && mobileInfo.isConnected());

        Log.i(TAG, "Connectivity changed. Wifi=" + wifiConnected + ". Mobile=" + mobileConnected);

        if (wifiConnected || mobileConnected) {

            String appState = AppStateUtils.getAppState(context);
            Log.i(TAG, "App State:"+appState);
            if(!appState.equals(SharedPrefUtils.STATE_LOGGED_OUT)) {
                Log.i(TAG, "Starting ServerProxyService...");
                Intent i = new Intent(context, ServerProxyService.class);
                i.setAction(ServerProxyService.ACTION_START);
                context.startService(i);
            }
        }
        else {
            Intent i = new Intent(context, ServerProxyService.class);
            i.setAction(ServerProxyService.ACTION_STOP);
            context.startService(i);
        }
    }
}
