package com_international.handlers.background_broadcast_receiver;

import android.content.Context;

import com_international.app.AppStateManager;
import com_international.handlers.Handler;
import com_international.mediacallz.app.R;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventCompressingHandler implements Handler {

    private static final String TAG = EventCompressingHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        String timeoutMsg = ctx.getResources().getString(R.string.compression_took_too_long);
        String loadingMsg = ctx.getResources().getString(R.string.compressing_file);
        AppStateManager.setLoadingState(ctx, TAG, loadingMsg, timeoutMsg);
    }
}
