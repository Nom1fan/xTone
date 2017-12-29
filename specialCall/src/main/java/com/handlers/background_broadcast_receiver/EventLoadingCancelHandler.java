package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com.app.AppStateManager;
import com.handlers.Handler;
import com.mediacallz.app.R;
import com.utils.UI_Utils;

import com.event.EventType;

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
