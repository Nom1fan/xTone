package com_international.handlers.background_broadcast_receiver;

import android.content.Context;

import com_international.app.AppStateManager;
import com_international.event.EventReport;
import com_international.event.EventType;
import com_international.handlers.Handler;
import com_international.mediacallz.app.R;
import com_international.utils.BroadcastUtils;

import java.util.HashMap;

import cz.msebera.android.httpclient.HttpStatus;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventRegisterFailureHandler implements Handler {

    private static final String TAG = EventRegisterFailureHandler.class.getSimpleName();
    private HashMap<Integer, String> resCode2String = new HashMap<>();

    @Override
    public void handle(Context ctx, Object... params) {
        EventReport eventReport = (EventReport) params[0];
        AppStateManager.setAppState(ctx, TAG, AppStateManager.STATE_IDLE);

        populateResCode2StringMap(ctx);

        int resCode = (int) eventReport.data();
        String msg = resCode2String.get(resCode);
        if(msg == null) {
            msg = ctx.getResources().getString(R.string.register_failure);
        }
        BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.REFRESH_UI, msg));
    }

    private void populateResCode2StringMap(Context ctx) {
        resCode2String.put(HttpStatus.SC_FORBIDDEN, ctx.getResources().getString(R.string.wrong_credentials));
        resCode2String.put(HttpStatus.SC_INTERNAL_SERVER_ERROR, ctx.getResources().getString(R.string.register_failure));
    }
}
