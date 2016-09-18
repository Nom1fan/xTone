package com.receivers;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.utils.PendingDownloadsUtils;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by mor on 01/10/2015.
 */
public class ConnectivityBroadcastReceiver extends WakefulBroadcastReceiver {

    private static final String TAG = ConnectivityBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean wifiConnected = false, mobileConnected = false;

        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                wifiConnected = true;
                PendingDownloadsUtils.handlePendingDownloads(context);
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                mobileConnected = true;
            }
        }

        log(Log.INFO,TAG, "Connectivity changed. Wifi=" + wifiConnected + ". Mobile=" + mobileConnected);
    }

}
