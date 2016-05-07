package com.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by Mor on 27/02/2016.
 */
public abstract class SpecialDevicesUtils {

    private static final String TAG = SpecialDevicesUtils.class.getSimpleName();
    private static final String[] StrictMemoryManagerDevices =
            {
                    "samsung sm-g920f" , "samsung sm-g920i" ,"samsung sm-g920w8"  // samsung galaxy S6 (has SPCM)
            };
    private static final String[] StrictRingingCapabilitiesDevices =
            {    // TODO maybe check LG G2 if it can have this strict ringing and if it good , than all LG models should be here , check for contains "lge" only and we cover all
                    "lge lg-h815" , "lge lg-h815t"  , "lge lg-d722" // LGE LG-H815 (LG G4) // TODO Add more LG models from LG G3 maybe we need also LG G5 or all LG devices , they are problematic . LG G2 is ok but above need to be added and QA to check it's good
            };


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

}
