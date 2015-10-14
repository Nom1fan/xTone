package com.utils;

import android.content.Context;
import android.util.Log;

/**
 * Created by mor on 01/10/2015.
 */
public class AppStateUtils {

    private static final String TAG = AppStateUtils.class.getSimpleName();

    /* Shared pref values under APP_STATE */
    public static final String STATE_LOGGED_OUT = "LoggedOut";
    public static final String STATE_LOGGED_IN = "LoggedIn";
    public static final String STATE_DISABLED = "Disabled";
    public static final String STATE_READY = "Ready";
    public static final String STATE_IDLE = "Idle";
    public static final String STATE_LOADING = "Loading";

    public synchronized static void setAppState(Context context, String tag , String state) {
        Log.i(TAG, tag + " Changing state to:"+state);
        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE, state);
    }

    public synchronized static String getAppState(Context context) {
        return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE);
    }
}
