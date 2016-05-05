package com.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.data_objects.Constants;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Mor on 27/02/2016.
 */
public abstract class InitUtils {

    private static final String TAG = InitUtils.class.getSimpleName();
    private static final String[] StrictMemoryManagerDevices =
            {
                    "samsung sm-g920f" , "samsung sm-g920i" ,"samsung sm-g920w8"  // samsung galaxy S6 (has SPCM)
            };
    private static final String[] StrictRingingCapabilitiesDevices =
            {
                    "lge lg-h815" , "lge lg-h815t"   // LGE LG-H815 (LG G4)
            };


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

    public static void checkIfDeviceHasStrictRingingCapabilitiesAndNeedMotivation(Context context)
    {
        String deviceName = getDeviceName();
        if (Arrays.asList(StrictRingingCapabilitiesDevices).contains(deviceName))
        {
            Log.i(TAG,"Device has strict Ringing Capabilities : " + deviceName);
            SharedPrefUtils.setBoolean(context , SharedPrefUtils.GENERAL , SharedPrefUtils.STRICT_RINGING_CAPABILITIES_DEVICES , true);
        }
        else
        {
            Log.i(TAG,"Device doesn't have strict Ringing Capabilities : " + deviceName);
            SharedPrefUtils.setBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.STRICT_RINGING_CAPABILITIES_DEVICES , false);
        }
    }

    public static void initializeSettingsDefaultValues(Context context) {

        SharedPrefUtils.setBoolean(context, SharedPrefUtils.SETTINGS, SharedPrefUtils.DOWNLOAD_ONLY_ON_WIFI, false);
        SharedPrefUtils.setBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.SAVE_MEDIA_OPTION, 0);

    }
}
