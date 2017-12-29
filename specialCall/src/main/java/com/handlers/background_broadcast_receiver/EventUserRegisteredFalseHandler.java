package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com.app.AppStateManager;
import com.handlers.Handler;
import com.mediacallz.app.R;
import com.utils.ContactsUtils;
import com.utils.ContactsUtilsImpl;
import com.utils.MediaFileUtils;
import com.utils.UI_Utils;

import com.event.EventReport;
import com.utils.UtilityFactory;

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
