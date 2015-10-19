package com.utils;

import android.content.Context;
import android.util.Log;

import EventObjects.EventReport;
import EventObjects.EventType;

/**
 * Created by mor on 01/10/2015.
 */
public class AppStateUtils {

    private static final String TAG = AppStateUtils.class.getSimpleName();
    private static final int LOADING_TIMEOUT = 20*1000;
    private static Thread loadingTimeoutThread;

    /* Shared pref values under APP_STATE */
    public static final String STATE_LOGGED_OUT = "LoggedOut";
    public static final String STATE_LOGGED_IN = "LoggedIn";
    public static final String STATE_DISABLED = "Disabled";
    public static final String STATE_READY = "Ready";
    public static final String STATE_IDLE = "Idle";
    public static final String STATE_LOADING = "Loading";

    public synchronized static void setAppState(Context context, String tag , String state) {
        Log.i(TAG, tag + " Changing state to:" + state);
        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE, state);

        if(state.equals(STATE_LOADING))
            setLoadingTimeout(context);
        else
            stopLoadingTimeout();
    }

    public synchronized static String getAppState(Context context) {
        return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE);
    }

    private synchronized static void setLoadingTimeout(final Context context) {

        loadingTimeoutThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(LOADING_TIMEOUT);
                    if(getAppState(context).equals(STATE_LOADING)) {
                        BroadcastUtils.sendEventReportBroadcast(context, "LOADING_TIMEOUT", new EventReport(EventType.LOADING_TIMEOUT, null, null));
                    }
                } catch (InterruptedException e) {
                    Log.i(TAG, "setLoadingTimeout interrupted, loading stopped before timeout");
                }


            }
        };
        loadingTimeoutThread.start();
    }

    private synchronized static void stopLoadingTimeout() {
        if(loadingTimeoutThread!=null)
            loadingTimeoutThread.interrupt();
        loadingTimeoutThread = null;
    }
}
