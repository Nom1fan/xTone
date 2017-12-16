package com_international.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com_international.data.objects.ClearSuccessData;
import com_international.handlers.Handler;
import com_international.mediacallz.app.R;
import com_international.utils.ContactsUtils;
import com_international.utils.LUT_Utils;
import com_international.utils.UI_Utils;

import com_international.event.EventReport;
import com_international.utils.UtilityFactory;

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

        UI_Utils.dismissTransferSuccessDialog();
        UI_Utils.showSnackBar(msg, Color.GREEN, Snackbar.LENGTH_LONG, false, ctx);
    }
}
