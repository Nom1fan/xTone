package com.app;

import android.content.Context;
import android.util.Log;

import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;

import EventObjects.EventReport;
import EventObjects.EventType;

/**
 * Created by mor on 01/10/2015.
 */
public class AppStateManager {

    private static final String TAG = AppStateManager.class.getSimpleName();
    private static Thread loadingTimeoutThread;

    /* Shared pref values under APP_STATE */
    public static final String STATE_LOGGED_OUT = "LoggedOut";
    public static final String STATE_LOGGED_IN = "LoggedIn";
    public static final String STATE_DISABLED = "Disabled";
    public static final String STATE_READY = "Ready";
    public static final String STATE_IDLE = "Idle";
    public static final String STATE_LOADING = "Loading";

    private static final int MAXIMUM_TIMEOUT_IN_MILLISECONDS = 10*1000;

    private static class LoadingState {

        private String _timeoutMsg;
        private int _loadingTimeout;

        public LoadingState(String timeoutMsg, int loadingTimeoutInMilliseconds) {

            _timeoutMsg = timeoutMsg;
            if(loadingTimeoutInMilliseconds > MAXIMUM_TIMEOUT_IN_MILLISECONDS)
                _loadingTimeout = MAXIMUM_TIMEOUT_IN_MILLISECONDS;
            else
                _loadingTimeout = loadingTimeoutInMilliseconds;
        }

        public int get_loadingTimeout() {
            return _loadingTimeout;
        }

        public String get_timeoutMsg() {
            return _timeoutMsg;
        }
    }

    public synchronized static void setAppState(Context context, String tag , String state) {
        Log.i(TAG, tag + " Changing state to:" + state);
        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE, state);

        stopLoadingTimeout();
    }

    public synchronized static void setAppState(Context context, String tag, LoadingState loadingState) {

        Log.i(TAG, tag + " Changing state to:" + STATE_LOADING);
        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE, STATE_LOADING);
        setLoadingTimeout(context, loadingState);
    }

    public synchronized static String getAppState(Context context) {
        return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE);
    }

    public synchronized static LoadingState createLoadingState(String timeoutMsg, int timeoutInMilliseconds) {

        return new LoadingState(timeoutMsg, timeoutInMilliseconds);
    }

    private synchronized static void setLoadingTimeout(final Context context, final LoadingState loadingState) {

        loadingTimeoutThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(loadingState.get_loadingTimeout());
                    if(getAppState(context).equals(STATE_LOADING)) {
                        BroadcastUtils.sendEventReportBroadcast(context, "LOADING_TIMEOUT", new EventReport(EventType.LOADING_TIMEOUT, loadingState.get_timeoutMsg(), null));
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
