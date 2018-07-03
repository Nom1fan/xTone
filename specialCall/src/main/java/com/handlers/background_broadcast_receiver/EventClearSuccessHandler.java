package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com.data.objects.ClearSuccessData;
import com.handlers.Handler;
import com.mediacallz.app.R;
import com.utils.ContactsUtils;
import com.utils.LUT_Utils;
import com.utils.UI_Utils;

import com.event.EventReport;
import com.utils.UtilityFactory;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventClearSuccessHandler implements Handler {

    private static final String TAG = EventClearSuccessHandler.class.getSimpleName();

    private final ContactsUtils contactsUtils = UtilityFactory.instance().getUtility(ContactsUtils.class);

    @Override
    public void handle(Context ctx, Object... params) {
        EventReport eventReport = (EventReport) params[0];

        // Preparing data for uploaded media thumbnail removal
        ClearSuccessData clearSuccessData = (ClearSuccessData) eventReport.data();
        LUT_Utils lut_utils = new LUT_Utils(clearSuccessData.getSpecialMediaType());
        String destId = clearSuccessData.getDestinationId();
        lut_utils.removeUploadedMediaPerNumber(ctx, destId);
        lut_utils.removeUploadedTonePerNumber(ctx, destId);
        
        String msg = String.format(ctx.getResources().getString(R.string.destination_media_cleared),
                contactsUtils.getContactNameHtml(ctx, destId));

        UI_Utils.dismissWaitingForTransferSuccessDialog();
        UI_Utils.showSnackBar(msg, Color.GREEN, Snackbar.LENGTH_LONG, false, ctx);
    }
}
