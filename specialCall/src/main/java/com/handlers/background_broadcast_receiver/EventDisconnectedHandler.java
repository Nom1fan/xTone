package com.handlers.background_broadcast_receiver;

import android.content.Context;

import com.app.AppStateManager;
import com.handlers.Handler;
import com.utils.BroadcastUtils;

import EventObjects.EventReport;
import EventObjects.EventType;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventDisconnectedHandler implements Handler {

    private static final String TAG = EventDisconnectedHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        AppStateManager.setAppState(ctx, TAG + " DISCONNECTED", AppStateManager.STATE_DISABLED);
        BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.REFRESH_UI));
    }
}
