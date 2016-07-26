package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com.app.AppStateManager;
import com.handlers.Handler;
import com.mediacallz.app.R;
import com.utils.CacheUtils;
import com.utils.ContactsUtils;
import com.utils.UI_Utils;

import EventObjects.EventReport;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventUserRegisteredTrueHandler implements Handler {

    private static final String TAG = EventUserRegisteredTrueHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        EventReport eventReport = (EventReport) params[0];

        AppStateManager.setAppState(ctx, TAG, AppStateManager.STATE_READY);

        String destNumber = (String) eventReport.data();
        if(destNumber!=null) {
            String msg = String.format(ctx.getResources().getString(R.string.user_is_registered),
                    ContactsUtils.getContactNameHtml(ctx, destNumber));
            CacheUtils.setPhone(ctx, destNumber);

            UI_Utils.showSnackBar(msg, Color.GREEN, Snackbar.LENGTH_LONG, false, ctx);
        }
    }
}
