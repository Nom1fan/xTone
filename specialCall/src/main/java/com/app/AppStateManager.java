package com.app;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.utils.SharedPrefUtils;

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
    public static final String STATE_LOADING    = "Loading";
    public static final String APP_IN_FG        = "AppInForeground";
    //endregion

    /**
     * Sets all states except for loading states
     *
     * @param context
     * @param tag     The component that sets the state
     * @param state   The app state to set
     */
    public synchronized static void setAppState(Context context, String tag, String state) {

        String curState = SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE);
        Crashlytics.log(Log.INFO,TAG, tag + " changes state from [" + curState + "] to: [" + state + "]");
        saveCurrAppState(context, curState);
        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE, state);
    }

    /**
     * Sets a loading state for the app.
     * @param context The application context
     * @param tag The caller tag
     * @param loadingMsg The message to be displayed on the UI while in the loading state
     */
    public synchronized static void setLoadingState(Context context, String tag, String loadingMsg, String timeoutMsg) {

        setLoadingMsg(context, loadingMsg);
        setTimeoutMsg(context , timeoutMsg);

        String curState = getAppState(context);
        Crashlytics.log(Log.INFO,TAG, tag + " changes state from [" + curState + "] to: [" + STATE_LOADING + "]");

        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE, STATE_LOADING);
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

    public static void setLoadingMsg(Context context, String loadingMsg) {

        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, loadingMsg);
    }

    public static void setTimeoutMsg(Context context, String timeoutMsg) {

        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.TIMEOUT_MSG, timeoutMsg);
    }

    public static String getTimeoutMsg(Context context) {

        return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.TIMEOUT_MSG);
    }

    private static void saveCurrAppState(Context context, String curState) {

        if (isNonBlockingState(curState)) {

            if(curState.isEmpty()) {
                Log.d(TAG, "saveCurrAppState:" + STATE_IDLE);
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_PREV_STATE, STATE_IDLE);
            }
            else {
                Log.d(TAG, "saveCurrAppState:" + curState);
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

    public static void setDidAppCrash(Context context, boolean b) {
        SharedPrefUtils.setBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.DID_APP_CRASH, b);
    }

    public static boolean didAppCrash(Context context) {
        return SharedPrefUtils.getBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.DID_APP_CRASH);
    }
}
