package com.xtone.logging;

import android.util.Log;
import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 31/05/2017.
 */

public class CrashlyticsLogger implements Logger {
    @Override
    public void info(String TAG, String msg) {
        log(Log.INFO, TAG, msg);
    }

    @Override
    public void warn(String TAG, String msg) {
        log(Log.WARN, TAG, msg);
    }

    @Override
    public void debug(String TAG, String msg) {
        log(Log.DEBUG, TAG, msg);
    }

    @Override
    public void error(String TAG, String msg) {
        log(Log.ERROR, TAG, msg);
    }
}
