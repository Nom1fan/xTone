package com_international.utils;

import android.content.Context;

/**
 * Created by Mor on 02/06/2017.
 */

public interface AlarmUtils extends Utility {

    String ALARM_INTENT_ACTION = "com.android.mediacallz.ALARM_ACTION";

    void setAlarm(Context context, Class aClass, long repeatTime);

    void setAlarm(Context context, Class aClass, long repeatTime, String action);
}
