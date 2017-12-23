package com.handlers.push.service;

import android.content.Context;
import android.content.Intent;

import com.data.objects.ClearMediaData;
import com.data.objects.ClearSuccessData;
import com.event.EventReport;
import com.event.EventType;
import com.google.firebase.messaging.RemoteMessage;
import com.handlers.AbstractPushHandler;
import com.services.ClearMediaIntentService;
import com.utils.BroadcastUtils;
import com.utils.NotificationUtils;

/**
 * Created by Mor on 12/31/2016.
 */

public class PushClearSuccessHandler extends AbstractPushHandler {

    public static final String TAG = PushClearSuccessHandler.class.getSimpleName();

    @Override
    public void handlePush(Context ctx, String pushData, Object... extraParams) {
        ClearSuccessData clearSuccessData = gson.fromJson(pushData, ClearSuccessData.class);
        RemoteMessage remoteMessage = (RemoteMessage) extraParams[0];

        BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.CLEAR_SUCCESS, clearSuccessData));
        NotificationUtils.displayNotificationInBgOnly(ctx, remoteMessage.getNotification());
    }
}
