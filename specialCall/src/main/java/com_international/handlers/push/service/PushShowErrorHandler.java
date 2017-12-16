package com_international.handlers.push.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com_international.data.objects.PushNotificationData;
import com_international.event.EventType;
import com_international.handlers.AbstractPushHandler;
import com_international.utils.NotificationUtils;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 12/31/2016.
 */

public class PushShowErrorHandler extends AbstractPushHandler {

    public static final String TAG = PushShowErrorHandler.class.getSimpleName();

    @Override
    public void handlePush(Context ctx, String pushData, Object... extraParams) {
        Intent intent = (Intent) extraParams[0];
        PushNotificationData pushNotificationData = gson.fromJson(pushData, PushNotificationData.class);
        log(Log.INFO, TAG, "Handling show error");
        NotificationUtils.displayNotification(ctx, pushNotificationData, intent, EventType.DISPLAY_ERROR);
    }
}
