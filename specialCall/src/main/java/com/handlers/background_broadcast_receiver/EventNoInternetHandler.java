package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com.app.AppStateManager;
import com.handlers.Handler;
import com.utils.BroadcastUtils;
import com.utils.UI_Utils;

import EventObjects.EventReport;
import EventObjects.EventType;

/**
 * Created by Mor on 16/09/2016.
 */
public class EventNoInternetHandler implements Handler {

    private static final String TAG = EventNoInternetHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        EventReport eventReportNoInternet = (EventReport) params[0];
        AppStateManager.setAppState(ctx, TAG, AppStateManager.getAppPrevState(ctx));

        if (AppStateManager.isLoggedIn(ctx)) {
            UI_Utils.showSnackBar(eventReportNoInternet.desc(), Color.RED, Snackbar.LENGTH_LONG, false, ctx);
        } else {
            EventReport refreshUIReport = new EventReport(EventType.REFRESH_UI, eventReportNoInternet.desc());
            BroadcastUtils.sendEventReportBroadcast(ctx, TAG, refreshUIReport);
        }
    }
}
