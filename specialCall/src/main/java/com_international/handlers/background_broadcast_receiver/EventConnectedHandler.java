package com_international.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com_international.app.AppStateManager;
import com_international.handlers.Handler;
import com_international.mediacallz.app.R;
import com_international.utils.BroadcastUtils;
import com_international.utils.UI_Utils;

import com_international.event.EventReport;
import com_international.event.EventType;

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
