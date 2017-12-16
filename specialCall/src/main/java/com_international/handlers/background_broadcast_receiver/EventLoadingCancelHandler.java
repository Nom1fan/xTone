package com_international.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com_international.app.AppStateManager;
import com_international.handlers.Handler;
import com_international.mediacallz.app.R;
import com_international.utils.UI_Utils;

import com_international.event.EventType;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventLoadingCancelHandler implements Handler {

    private static final String TAG = EventLoadingCancelHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        AppStateManager.setAppPrevState(ctx, TAG + " " + EventType.LOADING_CANCEL);
        String msg = ctx.getResources().getString(R.string.action_cancelled);
        UI_Utils.showSnackBar(msg, Color.YELLOW, Snackbar.LENGTH_LONG, false, ctx);
    }
}
