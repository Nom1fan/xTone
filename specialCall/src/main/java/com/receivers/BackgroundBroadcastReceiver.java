package com.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.handlers.Handler;
import com.handlers.HandlerFactory;

import com.event.Event;
import com.event.EventReport;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 01/10/2015.
 */
public class BackgroundBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = BackgroundBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        EventReport report = (EventReport) intent.getSerializableExtra(Event.EVENT_REPORT);
        log(Log.INFO, TAG, "Receiving event:" + report.status());

        Handler handler = HandlerFactory.getInstance().getHandler(TAG, report.status());
        if (handler != null) {
            handler.handle(context, report);
        }
    }
}
