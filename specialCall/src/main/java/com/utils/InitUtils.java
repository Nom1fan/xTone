package com.utils;

import android.content.Context;
import android.util.Log;

import com.data_objects.Constants;

import java.io.File;
import java.io.IOException;

/**
 * Created by Mor on 27/02/2016.
 */
public abstract class InitUtils {

    private static final String TAG = InitUtils.class.getSimpleName();

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

    public static void initializeSettingsDefaultValues(Context context) {

        SharedPrefUtils.setBoolean(context, SharedPrefUtils.SETTINGS, SharedPrefUtils.DOWNLOAD_ONLY_ON_WIFI, false);
        SharedPrefUtils.setInt(context, SharedPrefUtils.GENERAL, SharedPrefUtils.SAVE_MEDIA_OPTION, 0);

    }
}
