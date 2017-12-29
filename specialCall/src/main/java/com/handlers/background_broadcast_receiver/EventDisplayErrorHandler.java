package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com.handlers.Handler;
import com.utils.UI_Utils;

import com.event.EventReport;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventDisplayErrorHandler implements Handler {

    private static final String TAG = EventDisplayErrorHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        EventReport eventReport = (EventReport) params[0];
        UI_Utils.showSnackBar(eventReport.desc(), Color.RED, Snackbar.LENGTH_INDEFINITE, false, ctx);
    }
}
