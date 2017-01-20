package com.handlers.push.service;

import android.content.Context;
import android.content.Intent;

import com.data.objects.PendingDownloadData;
import com.event.EventReport;
import com.event.EventType;
import com.handlers.AbstractPushHandler;
import com.utils.BroadcastUtils;
import com.utils.NotificationUtils;

/**
 * Created by Mor on 12/31/2016.
 */

public class PushTransferFailureHandler extends AbstractPushHandler {

    public static final String TAG = PushTransferFailureHandler.class.getSimpleName();

    @Override
    public void handlePush(Context ctx, String pushData, Object... extraParams) {
        PendingDownloadData pendingDownloadData = gson.fromJson(pushData, PendingDownloadData.class);
        Intent intent = (Intent) extraParams[0];

        BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.DESTINATION_DOWNLOAD_FAILED, pendingDownloadData));
        NotificationUtils.displayNotificationInBgOnly(ctx, intent);
    }
}
