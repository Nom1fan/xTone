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

    private static final String TAG = AppStateManager.class.getSimpleName();

    //region Shared prefs values under APP_STATE
    public static final String IS_LOGGED_IN     = "IsLoggedIn";
    public static final String STATE_DISABLED   = "Disabled";
    public static final String STATE_READY      = "Ready";
    public static final String STATE_IDLE       = "Idle";
    public static final String APP_IN_FG        = "AppInForeground";
    //endregion

    //region Constants
    public static final String STATE_LOADING = "Loading";
    public static final int MAXIMUM_TIMEOUT_IN_MILLISECONDS = 60 * 1000;
    //endregion

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
     * @param loadingState The loading state. {@link LoadingState}
     */
    public synchronized static void setAppState(Context context, String tag, LoadingState loadingState, String loadingMsg) {

        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, loadingMsg);

        String curState = getAppState(context);
        Log.i(TAG, tag + " changes state from [" + curState + "] to: [" + STATE_LOADING + "]");

        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE, STATE_LOADING);
        if (loadingState.get_loadingTimeout() > 0)
            setLoadingTimeout(context, loadingState);
    }

    public static String getAppState(Context context) {
        return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE);
    }

    public static String getAppPrevState(Context context) {
        return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_PREV_STATE);
    }

    public synchronized static void setAppPrevState(Context context, String tag) {

        setAppState(context, tag, getAppPrevState(context));
    }

    public static boolean isNonBlockingState(Context context) {

        return !getAppState(context).equals(AppStateManager.STATE_DISABLED) &&
                !getAppState(context).equals(AppStateManager.STATE_LOADING);
    }

    public static boolean isNonBlockingState(String state) {

        return !state.equals(STATE_DISABLED) && !state.equals(STATE_LOADING);
    }

    public static boolean isBlockingState(String state) {

        return state.equals(STATE_DISABLED) || state.equals(STATE_LOADING);
    }

    public static String getLoadingMsg(Context context) {

        return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE);
    }

    public static boolean isLoadingStateActive() {

        return mLoadingTimeoutThread!=null && mLoadingTimeoutThread.isAlive();
    }

    /**
     * Creates a loading state for the app.
     *
     * @param eventReport           The event to be thrown in case the loading state's timeout has been reached.
     * @param timeoutInMilliseconds The timeout in milliseconds until the eventReport is sent.
     * @return The created loading state {@link LoadingState}
     */
    public static LoadingState createLoadingState(EventReport eventReport, int timeoutInMilliseconds) {

        return new LoadingState(eventReport, timeoutInMilliseconds);
    }

    private static void setLoadingTimeout(final Context context, final LoadingState loadingState) {

        Log.d(TAG, "Setting loading timeout. [Timeout in milliseconds]:" + loadingState.get_loadingTimeout() +
                " [Event to fire after timeout]:" + loadingState.get_eventReport().status());

        mLoadingTimeoutThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(loadingState.get_loadingTimeout());
                    if (getAppState(context).equals(STATE_LOADING)) {
                        Log.w(TAG, "Loading reached its timeout! Setting app state back to previous state...");
                        setAppState(context, TAG, getAppPrevState(context));
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

    private static void stopLoadingTimeout() {

        Log.d(TAG, "stopLoadingTimeout()");

        if (mLoadingTimeoutThread != null) {
            mLoadingTimeoutThread.interrupt();
        }
        mLoadingTimeoutThread = null;
    }

    private static void saveCurrAppState(Context context, String curState) {

        if (isNonBlockingState(curState)) {

            if(curState.isEmpty()) {
                Log.v(TAG, "saveCurrAppState:" + STATE_IDLE);
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_PREV_STATE, STATE_IDLE);
            }
            else {
                Log.v(TAG, "saveCurrAppState:" + curState);
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_PREV_STATE, curState);
            }
        }
    }

    public static void setAppInForeground(Context context, boolean b) {

        SharedPrefUtils.setBoolean(context, SharedPrefUtils.APP_STATE, APP_IN_FG, b);
    }

    public static boolean isAppInForeground(Context context) {

        return SharedPrefUtils.getBoolean(context, SharedPrefUtils.APP_STATE, APP_IN_FG);
    }

    public static void setIsLoggedIn(Context context, boolean b) {

        SharedPrefUtils.setBoolean(context, SharedPrefUtils.APP_STATE, IS_LOGGED_IN, b);
    }

    public static boolean isLoggedIn(Context context) {

        return SharedPrefUtils.getBoolean(context, SharedPrefUtils.APP_STATE, IS_LOGGED_IN);
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
