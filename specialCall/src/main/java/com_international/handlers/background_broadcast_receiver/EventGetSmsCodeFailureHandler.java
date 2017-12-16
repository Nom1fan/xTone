package com_international.handlers.background_broadcast_receiver;

import android.content.Context;

import com_international.app.AppStateManager;
import com_international.handlers.Handler;
import com_international.mediacallz.app.R;
import com_international.utils.BroadcastUtils;

import com_international.event.EventReport;
import com_international.event.EventType;

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
