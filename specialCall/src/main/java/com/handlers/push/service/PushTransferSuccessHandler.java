package com.handlers.push.service;

import android.content.Context;
import android.content.Intent;

import com.data.objects.PendingDownloadData;
import com.event.EventReport;
import com.event.EventType;
import com.google.firebase.messaging.RemoteMessage;
import com.handlers.AbstractPushHandler;
import com.utils.BroadcastUtils;
import com.utils.NotificationUtils;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 12/31/2016.
 */

public class PushTransferSuccessHandler extends AbstractPushHandler {

    public static final String TAG = PushTransferSuccessHandler.class.getSimpleName();

    @Override
    public void handlePush(Context ctx, String pushData, Object... extraParams) {
        PendingDownloadData pendingDownloadData = gson.fromJson(pushData, PendingDownloadData.class);
        RemoteMessage remoteMessage = (RemoteMessage) extraParams[0];

        BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.DESTINATION_DOWNLOAD_COMPLETE, pendingDownloadData));
        NotificationUtils.displayNotificationInBgOnly(ctx, remoteMessage.getNotification());
    }
}
