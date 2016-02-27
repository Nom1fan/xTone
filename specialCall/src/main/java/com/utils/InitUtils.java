package com.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.special.app.R;
import com.ui.activities.MainActivity;

import java.io.File;
import java.io.IOException;

import DataObjects.SharedConstants;

/**
 * Created by Mor on 27/02/2016.
 */
public abstract class InitUtils {

    private static final String TAG = InitUtils.class.getSimpleName();

    public static void addShortcutIcon(Context context) {
        //shorcutIntent object
        Intent shortcutIntent = new Intent(context,
                MainActivity.class);

        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        //shortcutIntent is added with addIntent
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, SharedConstants.APP_NAME);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(context,
                        R.drawable.color_mc));
        addIntent.putExtra("duplicate", false); // Just create once
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        // finally broadcast the new Intent
        context.sendBroadcast(addIntent);
    }

    public static void removeShortcutIcon(Context context) {

        Intent shortcutIntent = new Intent(context,
                MainActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "MediaCallz");

        addIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
        context.sendBroadcast(addIntent);
    }

    public static void hideMediaFromGalleryScanner(String path) {

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
}
