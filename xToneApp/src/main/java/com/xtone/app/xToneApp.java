package com.xtone.app;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;

import io.fabric.sdk.android.Fabric;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by mor on 10/09/2015.
 */
public class xToneApp extends Application {

    private static final Logger log = LoggerFactory.getLogger();

    private static final String TAG = xToneApp.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        log.info(TAG, "onCreate()");

        android.os.Process.setThreadPriority(-20);
        Context context = getApplicationContext();

        setupHandlerForUncaughtExceptions(context);

        // this must be after the setupHandlerForUncaughtExceptions so it will send the exceptions before it kill process
    }

    private void setupHandlerForUncaughtExceptions(final Context context) {
        // Setup handler for uncaught exceptions.
        Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException (Thread thread, Throwable e)
            {
                handleUncaughtException(context, e);
            }
        });
    }

    private void handleUncaughtException(Context context, Throwable e) {
        log.info(TAG, "Process terminated due to uncaught exception");
        e.printStackTrace();
    }

}
