package com.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewConfiguration;
import android.widget.TextView;
import android.widget.Toast;

import com.batch.android.Batch;
import com.batch.android.Config;
import com.data_objects.Constants;
import com.services.GetTokenIntentService;
import com.special.app.R;

import com.ui.activities.MainActivity;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import DataObjects.SharedConstants;

/**
 * Created by mor on 10/09/2015.
 */
public class MediaCallzApp extends Application {

    private static final String TAG = MediaCallzApp.class.getSimpleName();
    @Override
    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        try {

            // Initializing app state
            if (AppStateManager.getAppState(context).equals("")) {

                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_LOGGED_OUT);
                //make sure TitleBar Menu Appears in all devices (don't matter if they have HARD menu button or not)
                makeActionOverflowMenuShown();
                addShortcutIcon();

                // Initializing SQLite db
                // DAL_Manager.initialize(getApplicationContext());

                // Initializing Batch for push notifications
                Batch.Push.setGCMSenderId(Constants.GCM_SENDER_ID);
                Batch.Push.setManualDisplay(true);
                Batch.setConfig(new Config(SharedConstants.LIVE_API_KEY));

                Intent i = new Intent(context, GetTokenIntentService.class);
                i.setAction(GetTokenIntentService.ACTION_GET_BATCH_TOKEN);
                context.startService(i);

                // This will prevent Android's media scanner from reading your media files and including them in apps like Gallery or Music.
                hideMediaFromGalleryScanner(Constants.INCOMING_FOLDER);
                hideMediaFromGalleryScanner(Constants.OUTGOING_FOLDER);
                hideMediaFromGalleryScanner(Constants.TEMP_COMPRESSED_FOLDER);

            }
        } catch (Exception e) {
            String errMsg = "Failed to initialize. Please try to install again. Error:" + (e.getMessage()!=null ? e.getMessage() : e);
            callToast(errMsg, Color.RED);;
        } finally {
            context = null;
        }

    }

    private void makeActionOverflowMenuShown() {
        //devices with hardware menu button (e.g. Samsung Note) don't show action overflow menu
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
        }
    }

    private void addShortcutIcon() {
        //shorcutIntent object
        Intent shortcutIntent = new Intent(getApplicationContext(),
                MainActivity.class);

        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        //shortcutIntent is added with addIntent
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, SharedConstants.APP_NAME);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.drawable.color_mc));
        addIntent.putExtra("duplicate", false); // Just create once
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        // finally broadcast the new Intent
        getApplicationContext().sendBroadcast(addIntent);
    }

    private void removeShortcutIcon() {

        Intent shortcutIntent = new Intent(getApplicationContext(),
                MainActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "MediaCallz");

        addIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(addIntent);
    }

    private void hideMediaFromGalleryScanner(String path) {

        Log.i(TAG, "create file : " + path + "/" + ".nomedia");

        File new_file = new File(path + "/" + ".nomedia");  // This will prevent Android's media scanner from reading your media files and including them in apps like Gallery or Music.
        try {
            if (new_file.createNewFile())
                Log.i(TAG, ".nomedia Created !");
            else
                Log.i(TAG, ".nomedia Already Exists !");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void callToast(final String text, final int g) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(), text,
                        Toast.LENGTH_SHORT);
                TextView v = (TextView) toast.getView().findViewById(
                        android.R.id.message);
                v.setTextColor(g);
                toast.show();
            }
        });

    }
}
