package com.receivers;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.services.PushService;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 05/02/2016.
 */
public class PushReceiver extends WakefulBroadcastReceiver {

    private static final String TAG = PushReceiver.class.getSimpleName();

    public void onReceive(Context context, Intent intent) {

        if (intent !=null)
            log(Log.INFO,TAG, "PushReceiver  ACTION: " + intent.getAction() + " DATA: " +intent.getData() +" DATASTRING: "+ intent.getDataString());
        else
            log(Log.INFO,TAG, "PushReceiver: INTENT NULL !!!");
        ComponentName componentName = new ComponentName(context.getPackageName(), PushService.class.getName());

        log(Log.INFO,TAG, "PushReceiver startWakefulService with componentName: " + componentName.getClassName());
        startWakefulService(context, intent.setComponent(componentName));
        setResultCode(Activity.RESULT_OK);
    }
}
