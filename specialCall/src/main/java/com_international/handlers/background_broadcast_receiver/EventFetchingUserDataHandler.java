package com_international.handlers.background_broadcast_receiver;

import android.content.Context;

import com_international.app.AppStateManager;
import com_international.handlers.Handler;
import com_international.mediacallz.app.R;

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
