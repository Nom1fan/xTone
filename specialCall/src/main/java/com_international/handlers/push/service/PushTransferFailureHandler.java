package com_international.handlers.push.service;

import android.content.Context;
import android.content.Intent;

import com_international.data.objects.PendingDownloadData;
import com_international.event.EventReport;
import com_international.event.EventType;
import com_international.handlers.AbstractPushHandler;
import com_international.utils.BroadcastUtils;
import com_international.utils.NotificationUtils;

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
