package com.app;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.batch.android.Batch;
import com.batch.android.Config;
import com.crashlytics.android.Crashlytics;
import com.data_objects.Constants;
import com.utils.InitUtils;
import com.utils.UI_Utils;

import DataObjects.SharedConstants;
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
        Fabric.with(this, new Crashlytics());

        android.os.Process.setThreadPriority(-20);
        Context context = getApplicationContext();

        setupHandlerForUncaughtExceptions(context);


        // Initializing Batch for push notifications
        Batch.Push.setGCMSenderId(Constants.GCM_SENDER_ID);
        Batch.Push.setManualDisplay(true);
        Batch.setConfig(new Config(SharedConstants.LIVE_API_KEY));

        try {

            // Initializing app state
            if (AppStateManager.getAppState(context).equals("")) {

                AppStateManager.setIsLoggedIn(this, false);

                if(isNetworkAvailable())
                    AppStateManager.setAppState(this, TAG, AppStateManager.STATE_IDLE);
                else
                    AppStateManager.setAppState(this, TAG, AppStateManager.STATE_DISABLED);

                //make sure TitleBar Menu Appears in all devices (don't matter if they have HARD menu button or not)
                UI_Utils.makeActionOverflowMenuShown(context);

                // This will prevent Android's media scanner from reading your media files and including them in apps like Gallery or Music.
                InitUtils.hideMediaFromGalleryScanner();

                //Initialize Default Settings Values
                InitUtils.initializeSettingsDefaultValues(context);

                //Populate SharedprefMEdia in case it's not the first time the app is installed, and you have saved media in the MediaCallz Outgoing/Incoming
                InitUtils.initializeLoadingSavedMCFromDiskToSharedPrefs(context);

                InitUtils.saveAndroidVersion(context);

            }
        } catch (Exception e) {
            String errMsg = "Failed to initialize. Please try to install again. Error:" + (e.getMessage()!=null ? e.getMessage() : e);
            UI_Utils.callToast(errMsg, Color.RED, getApplicationContext());
        } finally {
            context = null;
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
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    protected boolean isNetworkAvailable() {

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        return activeNetwork!=null && activeNetwork.isConnected();
    }
}
