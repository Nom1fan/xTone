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
public class EventFetchingUserDataHandler implements Handler {

    private static final String TAG = EventFetchingUserDataHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        String timeoutMsg = ctx.getResources().getString(R.string.oops_try_again);
        String loadingMsg = ctx.getResources().getString(R.string.fetching_user_data);
        AppStateManager.setLoadingState(ctx, TAG + " FETCHING_USER_DATA", loadingMsg, timeoutMsg);
    }
}
