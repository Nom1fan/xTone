package com.handlers.push.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.data.objects.PushNotificationData;
import com.event.EventType;
import com.google.firebase.messaging.RemoteMessage;
import com.handlers.AbstractPushHandler;
import com.utils.NotificationUtils;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 12/31/2016.
 */

public class PushShowErrorHandler extends AbstractPushHandler {

    public static final String TAG = PushShowErrorHandler.class.getSimpleName();

    @Override
    public void handlePush(Context ctx, String pushData, Object... extraParams) {
        RemoteMessage remoteMessage = (RemoteMessage) extraParams[0];
        PushNotificationData pushNotificationData = gson.fromJson(pushData, PushNotificationData.class);
        log(Log.INFO, TAG, "Handling show error");
        NotificationUtils.displayNotification(ctx, pushNotificationData, remoteMessage.getNotification(), EventType.DISPLAY_ERROR);
    }
}
