package com_international.handlers.push.service;

import android.content.Context;
import android.content.Intent;

import com_international.data.objects.ClearSuccessData;
import com_international.event.EventReport;
import com_international.event.EventType;
import com_international.handlers.AbstractPushHandler;
import com_international.utils.BroadcastUtils;
import com_international.utils.NotificationUtils;

/**
 * Created by Mor on 12/31/2016.
 */

public class PushClearSuccessHandler extends AbstractPushHandler {

    public static final String TAG = PushClearSuccessHandler.class.getSimpleName();

    @Override
    public void handlePush(Context ctx, String pushData, Object... extraParams) {
        ClearSuccessData clearSuccessData = gson.fromJson(pushData, ClearSuccessData.class);
        Intent intent = (Intent) extraParams[0];

        BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.CLEAR_SUCCESS, clearSuccessData));
        NotificationUtils.displayNotificationInBgOnly(ctx, intent);
    }
}
