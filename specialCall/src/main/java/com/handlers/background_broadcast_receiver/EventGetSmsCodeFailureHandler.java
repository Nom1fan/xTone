package com.handlers.background_broadcast_receiver;

import android.content.Context;

import com.app.AppStateManager;
import com.handlers.Handler;
import com.mediacallz.app.R;
import com.utils.BroadcastUtils;

import com.event.EventReport;
import com.event.EventType;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventGetSmsCodeFailureHandler implements Handler {

    private static final String TAG = EventGetSmsCodeFailureHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        AppStateManager.setAppState(ctx, TAG, AppStateManager.STATE_IDLE);
        String msg = ctx.getResources().getString(R.string.sms_code_failed);
        BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.REFRESH_UI, msg));
    }
}
