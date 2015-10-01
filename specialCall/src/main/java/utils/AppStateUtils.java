package utils;

import android.content.Context;
import android.util.Log;

import data_objects.SharedPrefUtils;

/**
 * Created by mor on 01/10/2015.
 */
public class AppStateUtils {

    private static final String TAG = AppStateUtils.class.getSimpleName();

    public static void setAppState(Context context, String tag , String state) {
        Log.i(TAG, tag + " Changing state to:"+state);
        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE, state);
    }

    public static String getAppState(Context context) {
        return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.APP_STATE);
    }
}
