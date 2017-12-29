package com.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.receivers.StartStandOutServicesFallBackReceiver;

import java.util.Calendar;

/**
 * Created by Mor on 22/05/2017.
 */

public class AlarmUtilsImpl implements AlarmUtils {

    /**
     * Sets an alarm for sending the intent action repeatedly
     *
     * @param context    application context
     * @param aClass     specific component to create the intent for
     * @param repeatTime repeat time in milliseconds
     */
    @Override
    public void setAlarm(Context context, Class aClass, long repeatTime) {
        setAlarm(context, aClass, repeatTime, ALARM_INTENT_ACTION);
    }

    /**
     * Sets an alarm for sending the intent action repeatedly
     *
     * @param context    application context
     * @param aClass     specific component to create the intent for
     * @param repeatTime repeat time in milliseconds
     * @param action     the intent action to create
     */
    @Override
    public void setAlarm(Context context, Class aClass, long repeatTime, String action) {
        AlarmManager service = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, aClass);
        i.setAction(action);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, i,
                PendingIntent.FLAG_CANCEL_CURRENT);


        Calendar cal = Calendar.getInstance();
        // Start 30 seconds after boot completed
        cal.add(Calendar.SECOND, 30);
        //
        // Fetch every 30 seconds
        // InexactRepeating allows Android to optimize the energy consumption
        service.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(), repeatTime, pending);
    }
}
