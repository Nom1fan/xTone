package com.app;

import android.content.Context;
import android.util.Log;

import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;

import EventObjects.EventReport;

/**
 * Created by mor on 01/10/2015.
 */
public class AppStateManager {

    /* Shared pref values under APP_STATE */
    public static final String STATE_LOGGED_OUT = "LoggedOut";
    public static final String STATE_LOGGED_IN = "LoggedIn";
    public static final String STATE_DISABLED = "Disabled";
    public static final String STATE_READY = "Ready";
    public static final String STATE_IDLE = "Idle";
    public static final String STATE_LOADING = "Loading";
    public static final int MAXIMUM_TIMEOUT_IN_MILLISECONDS = 20 * 1000;
    private static final String TAG = AppStateManager.class.getSimpleName();
    private static Thread mLoadingTimeoutThread = null;

    /**
     * Sets all states except for loading states
     *
     * @param context
     * @param tag     The component that sets the state
     * @param state   The app state to set
     */
    public synchronized static void setAppState(Context context, String tag, String state) {

        String curState = SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE);
        Log.i(TAG, tag + " changes state from [" + curState + "] to: [" + state + "]");
        saveCurrAppState(context, curState);
        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE, state);

        stopLoadingTimeout();
    }

    /**
     * Sets a loading state for the app.
     *
     * @param context
     * @param tag          The tag of the component setting the loading state
     * @param loadingState The loading state containing a loading timeout message and a timeout in milliseconds
     */
    public synchronized static void setAppState(Context context, String tag, LoadingState loadingState, String loadingMsg) {

        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, loadingMsg);

        String curState = getAppState(context);
        Log.i(TAG, tag + " changes state from [" + curState + "] to: [" + STATE_LOADING + "]");
        setAppState(context, tag, STATE_LOADING);
        if (loadingState.get_loadingTimeout() > 0)
            setLoadingTimeout(context, loadingState);
    }

    public synchronized static String getAppState(Context context) {
        return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE);
    }

    public synchronized static String getAppPrevState(Context context) {
        return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_PREV_STATE);
    }

    public synchronized static void setAppPrevState(Context context, String tag) {

        setAppState(context, tag, getAppPrevState(context));
    }

    public synchronized static boolean isNonBlockingState(Context context) {

        return !getAppState(context).equals(AppStateManager.STATE_DISABLED) &&
                !getAppState(context).equals(AppStateManager.STATE_LOADING);
    }

    public synchronized static String getLoadingMsg(Context context) {

        return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE);
    }

    public synchronized static boolean isLoadingStateActive() {

        return mLoadingTimeoutThread!=null && mLoadingTimeoutThread.isAlive();
    }

    /**
     * Creates a loading state for the app. To use without a timeout, set timeoutInMilliseconds to be <= 0
     *
     * @param eventReport           The event to be thrown in case the loading state's timeout has been reached.
     * @param timeoutInMilliseconds The timeout in milliseconds until the eventReport is sent.
     * @return
     */
    public synchronized static LoadingState createLoadingState(EventReport eventReport, int timeoutInMilliseconds) {

        return new LoadingState(eventReport, timeoutInMilliseconds);
    }

    private synchronized static void setLoadingTimeout(final Context context, final LoadingState loadingState) {

        Log.d(TAG, "Setting loading timeout. [Timeout in milliseconds]:" + loadingState.get_loadingTimeout() +
                " [Event to fire after timeout]:" + loadingState.get_eventReport().status());

        mLoadingTimeoutThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(loadingState.get_loadingTimeout());
                    if (getAppState(context).equals(STATE_LOADING)) {
                        Log.w(TAG, "Loading reached its timeout! Setting app state back to idle...");
                        setAppState(context, TAG, AppStateManager.STATE_IDLE);
                        BroadcastUtils.sendEventReportBroadcast(context, "LOADING_TIMEOUT", loadingState.get_eventReport());
                        mLoadingTimeoutThread = null;
                    }
                } catch (InterruptedException e) {
                    Log.i(TAG, "setLoadingTimeout interrupted, loading stopped before timeout");
                    mLoadingTimeoutThread = null;
                }
            }
        };
        mLoadingTimeoutThread.start();
    }

    private synchronized static void stopLoadingTimeout() {

        Log.d(TAG, "stopLoadingTimeout()");

        if (mLoadingTimeoutThread != null) {
            mLoadingTimeoutThread.interrupt();
        }
        mLoadingTimeoutThread = null;
    }

    private static void saveCurrAppState(Context context, String curState) {

        if (isNonBlockingState(context))
            SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_PREV_STATE, curState);
    }

    private static class LoadingState {

        private int _loadingTimeout;
        private EventReport _eventReport;

        public LoadingState(EventReport eventReport, int loadingTimeoutInMilliseconds) {

            _eventReport = eventReport;
            if (loadingTimeoutInMilliseconds > MAXIMUM_TIMEOUT_IN_MILLISECONDS)
                _loadingTimeout = MAXIMUM_TIMEOUT_IN_MILLISECONDS;
            else
                _loadingTimeout = loadingTimeoutInMilliseconds;
        }

        public int get_loadingTimeout() {
            return _loadingTimeout;
        }

        public EventReport get_eventReport() {
            return _eventReport;
        }
    }
}
