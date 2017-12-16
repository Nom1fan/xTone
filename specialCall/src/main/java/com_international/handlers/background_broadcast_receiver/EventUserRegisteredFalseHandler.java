package com_international.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com_international.app.AppStateManager;
import com_international.handlers.Handler;
import com_international.mediacallz.app.R;
import com_international.utils.ContactsUtils;
import com_international.utils.UI_Utils;

import com_international.event.EventReport;
import com_international.utils.UtilityFactory;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventUserRegisteredFalseHandler implements Handler {

    private static final String TAG = EventUserRegisteredFalseHandler.class.getSimpleName();

    private ContactsUtils contactsUtils = UtilityFactory.instance().getUtility(ContactsUtils.class);

    @Override
    public void handle(Context ctx, Object... params) {
        EventReport eventReport = (EventReport) params[0];

        AppStateManager.setAppState(ctx, TAG, AppStateManager.STATE_IDLE);

        String destNumber = (String) eventReport.data();
        String msg =  String.format(ctx.getResources().getString(R.string.user_is_unregistered),
                contactsUtils.getContactNameHtml(ctx, destNumber));

        UI_Utils.showSnackBar(msg, Color.GREEN, Snackbar.LENGTH_LONG, false, ctx);

    }
}
