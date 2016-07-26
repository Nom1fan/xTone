package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com.handlers.Handler;
import com.mediacallz.app.R;
import com.utils.ContactsUtils;
import com.utils.LUT_Utils;
import com.utils.UI_Utils;

import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.SpecialMediaType;
import EventObjects.EventReport;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventClearSuccessHandler implements Handler {

    private static final String TAG = EventClearSuccessHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        EventReport eventReport = (EventReport) params[0];

        // Preparing _data for uploaded media thumbnail removal
        Map data = (Map) eventReport.data();
        String destId = data.get(DataKeys.DESTINATION_ID).toString();
        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.valueOf(data.get(DataKeys.SPECIAL_MEDIA_TYPE).toString()));
        lut_utils.removeUploadedMediaPerNumber(ctx, destId);
        lut_utils.removeUploadedTonePerNumber(ctx, destId);
        
        String msg = String.format(ctx.getResources().getString(R.string.destination_media_cleared),
                ContactsUtils.getContactNameHtml(ctx, destId));

        UI_Utils.dismissTransferSuccessDialog();
        UI_Utils.showSnackBar(msg, Color.GREEN, Snackbar.LENGTH_LONG, false, ctx);
    }
}
