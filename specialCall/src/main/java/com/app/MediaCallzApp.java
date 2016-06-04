package com.app;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.batch.android.Batch;
import com.batch.android.Config;
import com.data_objects.Constants;
import com.utils.InitUtils;
import com.utils.UI_Utils;

import DataObjects.SharedConstants;

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

    protected boolean isNetworkAvailable() {

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        return activeNetwork!=null && activeNetwork.isConnected();
    }
}
