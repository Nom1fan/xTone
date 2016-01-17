package com.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.ViewConfiguration;

import com.services.GetTokenIntentService;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.SaveCallback;

import com.data_objects.Constants;
import com.special.app.R;
import com.ui.activities.MainActivity;
import com.utils.SharedPrefUtils;

import java.lang.reflect.Field;

/**
 * Created by mor on 10/09/2015.
 */
public class SpecialCallApp extends Application {

    private static final String TAG = SpecialCallApp.class.getSimpleName();
    @Override
    public void onCreate() {
        super.onCreate();

        final Context context = getApplicationContext();




        //make sure TitleBar Menu Appears in all devices (don't matter if they have HARD menu button or not)

            makeActionOverflowMenuShown();

        // Initializing app state
        if(AppStateManager.getAppState(context).equals("")) {
            addShortcutIcon();
            AppStateManager.setAppState(context, TAG, AppStateManager.STATE_LOGGED_OUT);
        }
        // Initializing SQLite db
//        DAL_Manager.initialize(getApplicationContext());

        // Initializing Parse for push notifications. NOTE: Also currently using device token as registration token
        Parse.initialize(this, Constants.APPLICATION_ID, Constants.CLIENT_KEY);
        if(getDeviceToken().equals("")) {
            ParsePush.subscribeInBackground("SpecialCall");
            ParseInstallation.getCurrentInstallation().saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {

                    Intent i = new Intent(context, GetTokenIntentService.class);
                    i.setAction(GetTokenIntentService.ACTION_GET_TOKEN);
                    context.startService(i);

                }
            });
        }
    }

    private String getDeviceToken() {

        return SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN);
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



    // onClick of addShortcutIcon
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
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "MediaCallz");
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.drawable.mediacallztempicon));
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


}
