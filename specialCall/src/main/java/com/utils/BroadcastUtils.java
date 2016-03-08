package com.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import EventObjects.Event;
import EventObjects.EventReport;

/**
 * Created by Mor on 15/10/2015.
 */
public abstract class BroadcastUtils {

    public static void sendEventReportBroadcast(Context context, String tag, EventReport report) {

        Log.i(tag, "Broadcasting event:" + report.status().toString());
        Intent broadcastEvent = new Intent(Event.EVENT_ACTION);
        broadcastEvent.putExtra(Event.EVENT_REPORT, report);
        context.sendBroadcast(broadcastEvent);
    }

    public static void sendCustomBroadcast(Context context, String tag, Intent i) {

        Log.i(tag, "Sending custom broadcast:" + i.getAction());
        context.sendBroadcast(i);
    }
}
