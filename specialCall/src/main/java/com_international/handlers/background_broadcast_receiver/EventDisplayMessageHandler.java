package com_international.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com_international.handlers.Handler;
import com_international.utils.UI_Utils;

import com_international.event.EventReport;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventDisplayMessageHandler implements Handler {

    private static final String TAG = EventDisplayMessageHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        EventReport eventReport = (EventReport) params[0];
        UI_Utils.showSnackBar(eventReport.desc(), Color.GREEN, Snackbar.LENGTH_INDEFINITE, false, ctx);
    }
}
