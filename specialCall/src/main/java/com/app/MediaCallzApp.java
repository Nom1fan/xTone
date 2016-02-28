package com.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import com.batch.android.Batch;
import com.batch.android.Config;
import com.data_objects.Constants;
import com.services.GetTokenIntentService;
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

                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_LOGGED_OUT);
                //make sure TitleBar Menu Appears in all devices (don't matter if they have HARD menu button or not)
                UI_Utils.makeActionOverflowMenuShown(context);
                InitUtils.addShortcutIcon(context);

                // Initializing SQLite db
                // DAL_Manager.initialize(getApplicationContext());

                Intent i = new Intent(context, GetTokenIntentService.class);
                i.setAction(GetTokenIntentService.ACTION_GET_BATCH_TOKEN);
                context.startService(i);

                // This will prevent Android's media scanner from reading your media files and including them in apps like Gallery or Music.
                InitUtils.hideMediaFromGalleryScanner(Constants.INCOMING_FOLDER);
                InitUtils.hideMediaFromGalleryScanner(Constants.OUTGOING_FOLDER);
                InitUtils.hideMediaFromGalleryScanner(Constants.TEMP_COMPRESSED_FOLDER);

            }
        } catch (Exception e) {
            String errMsg = "Failed to initialize. Please try to install again. Error:" + (e.getMessage()!=null ? e.getMessage() : e);
            UI_Utils.callToast(errMsg, Color.RED, getApplicationContext());
        } finally {
            context = null;
        }

    }

}
