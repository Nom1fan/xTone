package com.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.event.Event;
import com.event.EventReport;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 15/10/2015.
 */
public abstract class BroadcastUtils {

    public static void sendEventReportBroadcast(Context context, String tag, EventReport report) {

        log(Log.INFO,tag, "Broadcasting event:" + report.status().toString());
        Intent broadcastEvent = new Intent(Event.EVENT_ACTION);
        broadcastEvent.putExtra(Event.EVENT_REPORT, report);
        context.sendBroadcast(broadcastEvent);
    }

    public static void sendCustomBroadcast(Context context, String tag, Intent i) {

        log(Log.INFO,tag, "Sending custom broadcast:" + i.getAction());
        context.sendBroadcast(i);
    }
}
