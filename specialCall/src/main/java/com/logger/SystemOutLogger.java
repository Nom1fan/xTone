package com.logger;

import android.util.Log;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 31/05/2017.
 */

class SystemOutLogger implements Logger {
    @Override
    public void info(String TAG, String msg) {
        System.out.println("INFO:::" + TAG + ":::" + msg);
    }

    @Override
    public void warn(String TAG, String msg) {
        System.out.println("WARN:::" + TAG + ":::" + msg);
    }

    @Override
    public void debug(String TAG, String msg) {
        System.out.println("DEBUG:::" + TAG + ":::" + msg);
    }

    @Override
    public void error(String TAG, String msg) {
        System.out.println("ERROR:::" + TAG + ":::" + msg);
    }
}
