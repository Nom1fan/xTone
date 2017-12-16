package com_international.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com_international.event.Event;
import com_international.event.EventReport;

import java.util.Arrays;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 15/10/2015.
 */
public abstract class BroadcastUtils {

    public static void sendEventMultiReportBroadcast(Context context, String tag, EventReport... reports) {
        log(Log.INFO, tag, "Broadcasting event:" + Arrays.toString(reports));
        Intent broadcastEvent = new Intent(Event.EVENT_ACTION);

        for (int i = 0; i < reports.length; i++) {
            broadcastEvent.putExtra(String.valueOf(i), reports[i]);
        }

        context.sendBroadcast(broadcastEvent);
    }

    public static void sendEventReportBroadcast(Context context, String tag, EventReport report) {

        log(Log.INFO, tag, "Broadcasting event:" + report.status().toString());
        Intent broadcastEvent = new Intent(Event.EVENT_ACTION);
        broadcastEvent.putExtra(Event.EVENT_REPORT, report);
        context.sendBroadcast(broadcastEvent);
    }

    public static void sendCustomBroadcast(Context context, String tag, Intent i) {

        log(Log.INFO, tag, "Sending custom broadcast:" + i.getAction());
        context.sendBroadcast(i);
    }
}
