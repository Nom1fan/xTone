package com.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.data_objects.Constants;
import com.special.app.R;
import com.ui.activities.MainActivity;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import DataObjects.SharedConstants;

/**
 * Created by Mor on 27/02/2016.
 */
public abstract class InitUtils {

    private static final String TAG = InitUtils.class.getSimpleName();
    private static final String[] StrictMemoryManagerDevices =
            {
                    "samsung sm-g920f" , "samsung sm-g920i" ,"samsung sm-g920w8"  // samsung galaxy S6 (has SPCM)
            };


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

    public static void hideMediaFromGalleryScanner() {

        hideMediaFromGalleryScanner(Constants.INCOMING_FOLDER);
        hideMediaFromGalleryScanner(Constants.OUTGOING_FOLDER);
        hideMediaFromGalleryScanner(Constants.TEMP_COMPRESSED_FOLDER);
    }

    private static void hideMediaFromGalleryScanner(String path) {

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

    /** Returns the consumer friendly device name */
    private static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model.toLowerCase();
        }
        Log.i(TAG, "Manufacturer device name: " + manufacturer + " " + model);
        return (manufacturer + " " + model).toLowerCase();
    }

    public static void checkIfDeviceHasStrictMemoryManager(Context context)
    {
        String deviceName = getDeviceName();
        if (Arrays.asList(StrictMemoryManagerDevices).contains(deviceName))
        {
            Log.i(TAG,"Device has strict memory manager: " + deviceName);
            SharedPrefUtils.setBoolean(context , SharedPrefUtils.GENERAL , SharedPrefUtils.STRICT_MEMORY_MANAGER_DEVICES , true);
        }
        else
        {
            Log.i(TAG,"Device doesn't have strict memory manager: " + deviceName);
            SharedPrefUtils.setBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.STRICT_MEMORY_MANAGER_DEVICES , false);
        }
    }


}
