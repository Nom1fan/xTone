package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com.app.AppStateManager;
import com.handlers.Handler;
import com.mediacallz.app.R;
import com.utils.BroadcastUtils;
import com.utils.UI_Utils;

import EventObjects.EventReport;
import EventObjects.EventType;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventConnectedHandler implements Handler {

    private static final String TAG = EventConnectedHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        AppStateManager.setAppState(ctx, TAG, AppStateManager.getAppPrevState(ctx));
        String msg = ctx.getResources().getString(R.string.connected);

        if (AppStateManager.isLoggedIn(ctx)) {
            UI_Utils.showSnackBar(msg, Color.GREEN, Snackbar.LENGTH_LONG, false, ctx);
        } else {
            EventReport eventReport = new EventReport(EventType.REFRESH_UI, msg);
            BroadcastUtils.sendEventReportBroadcast(ctx, TAG, eventReport);
        }
    }
}
