package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com.app.AppStateManager;
import com.handlers.Handler;
import com.utils.BroadcastUtils;
import com.utils.UI_Utils;

import com.event.EventReport;
import com.event.EventType;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventLoadingTimeoutHandler implements Handler {

    private static final String TAG = EventLoadingTimeoutHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        AppStateManager.setAppPrevState(ctx, TAG);
        String timeoutMsg = AppStateManager.getTimeoutMsg(ctx);

        if (AppStateManager.isLoggedIn(ctx)) {
            UI_Utils.showSnackBar(timeoutMsg, Color.RED, Snackbar.LENGTH_LONG, false, ctx);
        } else {
            EventReport eventReport = new EventReport(EventType.REFRESH_UI, timeoutMsg);
            BroadcastUtils.sendEventReportBroadcast(ctx, TAG, eventReport);
        }
    }
}
