package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com.app.AppStateManager;
import com.handlers.Handler;
import com.mediacallz.app.R;
import com.utils.UI_Utils;

/**
 * Created by Mor on 17/07/2016.
 */
public class EventNegativeEventHandler implements Handler {

    private static final String TAG = EventNegativeEventHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        String msg = ctx.getResources().getString(R.string.oops_try_again);
        AppStateManager.setAppState(ctx, TAG, AppStateManager.STATE_IDLE);
        UI_Utils.showSnackBar(msg, Color.RED, Snackbar.LENGTH_INDEFINITE, false, ctx);
    }
}
