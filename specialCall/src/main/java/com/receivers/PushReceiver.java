package com.receivers;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import com.services.PushService;

/**
 * Created by Mor on 05/02/2016.
 */
public class PushReceiver extends WakefulBroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        ComponentName var3 = new ComponentName(context.getPackageName(), PushService.class.getName());
        startWakefulService(context, intent.setComponent(var3));
        setResultCode(Activity.RESULT_OK);
    }
}
