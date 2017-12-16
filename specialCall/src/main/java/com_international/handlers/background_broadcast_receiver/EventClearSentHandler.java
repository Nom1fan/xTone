package com_international.handlers.background_broadcast_receiver;

import android.content.Context;

import com_international.app.AppStateManager;
import com_international.handlers.Handler;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventClearSentHandler implements Handler {

    private static final String TAG = EventClearSentHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        AppStateManager.setAppState(ctx, TAG, AppStateManager.getAppPrevState(ctx));
    }
}
