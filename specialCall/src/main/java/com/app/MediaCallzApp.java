package com.app;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.utils.UI_Utils;

import io.fabric.sdk.android.Fabric;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by mor on 10/09/2015.
 */
public class MediaCallzApp extends Application {

    private static final String TAG = MediaCallzApp.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        android.os.Process.setThreadPriority(-20);
        Context context = getApplicationContext();

        setupHandlerForUncaughtExceptions(context);

        // this must be after the setupHandlerForUncaughtExceptions so it will send the exceptions before it kill process
        Fabric.with(this, new Crashlytics());

        try {

            // Initializing app state
            if (AppStateManager.getAppState(context).equals("")) {

                AppStateManager.setIsLoggedIn(this, false);
                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_IDLE);

                //make sure TitleBar Menu Appears in all devices (don't matter if they have HARD menu button or not)
                UI_Utils.makeActionOverflowMenuShown(context);

                // This will prevent Android's media scanner from reading your media files and including them in apps like Gallery or Music.
                InitUtils.hideMediaFromGalleryScanner();

                //Initialize Default Settings Values
                InitUtils.initializeSettingsDefaultValues(context);

                //Populate SharedprefMEdia in case it's not the first time the app is installed, and you have saved media in the MediaCallz Outgoing/Incoming
                InitUtils.populateSavedMcFromDiskToSharedPrefs(context);

                InitUtils.saveAndroidVersion(context);

                InitUtils.initImageLoader(context);

            }
        } catch (Exception e) {
            String errMsg = "Failed to initialize. Please try to install again. Error:" + (e.getMessage()!=null ? e.getMessage() : e);
            UI_Utils.callToast(errMsg, Color.RED, getApplicationContext());
        }

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
        AppStateManager.setDidAppCrash(context, true);

        log(Log.INFO, TAG, "Process you failed me! DIE PROCESS DIE !!!!");
        e.printStackTrace();
//        android.os.Process.killProcess(android.os.Process.myPid());
    }

}
