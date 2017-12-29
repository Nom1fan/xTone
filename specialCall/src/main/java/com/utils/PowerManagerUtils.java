package com.utils;

import android.content.Context;
import android.os.PowerManager;

/**
 * Created by Mor on 10/06/2017.
 */

public interface PowerManagerUtils extends Utility {

    PowerManager.WakeLock getWakeLock(Context context);
}
