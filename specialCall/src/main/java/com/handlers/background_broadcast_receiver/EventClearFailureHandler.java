package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com.app.AppStateManager;
import com.handlers.Handler;
import com.mediacallz.app.R;
import com.utils.UI_Utils;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventClearFailureHandler implements Handler {

    private static final String TAG = EventClearFailureHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        AppStateManager.setAppState(ctx, TAG, AppStateManager.getAppPrevState(ctx));
        String msg = ctx.getResources().getString(R.string.oops_try_again);
        UI_Utils.showSnackBar(msg, Color.RED, Snackbar.LENGTH_LONG, false, ctx);
    }
}
