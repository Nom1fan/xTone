package com.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;

import static com.crashlytics.android.Crashlytics.log;

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
            {    // TODO maybe check LG G2(= lge lg-d802  )if it can have this strict ringing and if it good , than all LG models should be here , check for contains "lge" only and we cover all
                    "lge lg-h815" , "lge lg-h815t"  , "lge lg-d722" // LGE LG-H815 (LG G4) //TODO QA The Shit out of it
            };

    private static final String StrictRingingCapabilitiesLGDevice = "lge lg";
    private static final String LG_G2_StrictRingingDisable = "lg-d802";

    /** Returns the consumer friendly device name */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model.toLowerCase();
        }
        log(Log.INFO,TAG, "Manufacturer device name: " + manufacturer + " " + model);
        return (manufacturer + " " + model).toLowerCase();
    }

    public static void checkIfDeviceHasStrictMemoryManager(Context context)
    {
        String deviceName = getDeviceName();
        if (Arrays.asList(StrictMemoryManagerDevices).contains(deviceName))
        {
            log(Log.INFO,TAG,"Device has strict memory manager: " + deviceName);
            SharedPrefUtils.setBoolean(context , SharedPrefUtils.GENERAL , SharedPrefUtils.STRICT_MEMORY_MANAGER_DEVICES , true);
        }
        else
        {
            log(Log.INFO,TAG,"Device doesn't have strict memory manager: " + deviceName);
            SharedPrefUtils.setBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.STRICT_MEMORY_MANAGER_DEVICES , false);
        }
    }

    public static void checkIfDeviceHasStrictRingingCapabilitiesAndNeedMotivation(Context context)
    {
        String deviceName = getDeviceName();
        if (deviceName.contains(StrictRingingCapabilitiesLGDevice) && !deviceName.contains(LG_G2_StrictRingingDisable) )  // all LG devices except LG G2
        {
            log(Log.INFO,TAG,"Device has strict Ringing Capabilities : " + deviceName);
            SettingsUtils.setStrictRingingCapabilitiesDevice(context, true);
        }
        else
        {
            log(Log.INFO,TAG,"Device doesn't have strict Ringing Capabilities : " + deviceName);
            SettingsUtils.setStrictRingingCapabilitiesDevice(context, false);
        }
    }

}
