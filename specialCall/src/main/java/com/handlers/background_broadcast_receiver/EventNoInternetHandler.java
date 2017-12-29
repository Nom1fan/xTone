package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com.app.AppStateManager;
import com.event.EventReport;
import com.event.EventType;
import com.handlers.Handler;
import com.mediacallz.app.R;
import com.utils.BroadcastUtils;
import com.utils.UI_Utils;

/**
 * Created by Mor on 16/09/2016.
 */
public class EventNoInternetHandler implements Handler {

    private static final String TAG = EventNoInternetHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        String msgNoInternet = ctx.getResources().getString(R.string.disconnected);
        AppStateManager.setAppState(ctx, TAG, AppStateManager.getAppPrevState(ctx));

        if (AppStateManager.isLoggedIn(ctx)) {
            UI_Utils.showSnackBar(msgNoInternet, Color.RED, Snackbar.LENGTH_INDEFINITE, false, ctx);
        } else {
            EventReport refreshUIReport = new EventReport(EventType.REFRESH_UI, msgNoInternet);
            BroadcastUtils.sendEventReportBroadcast(ctx, TAG, refreshUIReport);
        }
    }
}
