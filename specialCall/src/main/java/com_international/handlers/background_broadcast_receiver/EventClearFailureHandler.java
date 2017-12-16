package com_international.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com_international.app.AppStateManager;
import com_international.handlers.Handler;
import com_international.mediacallz.app.R;
import com_international.utils.UI_Utils;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventClearFailureHandler implements Handler {

    private static final String TAG = EventClearFailureHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        String msg = ctx.getResources().getString(R.string.oops_try_again);
        AppStateManager.setAppState(ctx, TAG, AppStateManager.getAppPrevState(ctx));
        UI_Utils.showSnackBar(msg, Color.RED, Snackbar.LENGTH_LONG, false, ctx);
    }
}
