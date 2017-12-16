package com_international.utils;

import android.content.Context;
import android.os.PowerManager;

import static android.content.Context.POWER_SERVICE;

/**
 * Created by Mor on 10/06/2017.
 */

class PowerManagerUtilsImpl implements PowerManagerUtils {

    private static String TAG = PowerManagerUtilsImpl.class.getSimpleName();

    @Override
    public PowerManager.WakeLock getWakeLock(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + "_wakeLock");
    }
}
