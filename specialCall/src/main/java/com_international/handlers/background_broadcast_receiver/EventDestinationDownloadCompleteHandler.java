package com_international.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com_international.data.objects.PendingDownloadData;
import com_international.handlers.Handler;
import com_international.mediacallz.app.R;
import com_international.utils.ContactsUtils;
import com_international.utils.LUT_Utils;
import com_international.utils.UI_Utils;

import com_international.event.EventReport;
import com_international.utils.UtilityFactory;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventDestinationDownloadCompleteHandler implements Handler {

    private static final String TAG = EventDestinationDownloadCompleteHandler.class.getSimpleName();

    private final ContactsUtils contactsUtils = UtilityFactory.instance().getUtility(ContactsUtils.class);

    @Override
    public void handle(Context ctx, Object... params) {
        EventReport eventReport = (EventReport) params[0];

        log(Log.INFO,TAG, "In: DESTINATION_DOWNLOAD_COMPLETE");
        // Preparing _data for uploaded media thumbnail display
        PendingDownloadData data = (PendingDownloadData) eventReport.data();
        String destId = data.getDestinationId();
        LUT_Utils lut_utils = new LUT_Utils(data.getSpecialMediaType());
        lut_utils.saveUploadedPerNumber(ctx, destId, data.getFilePathOnSrcSd());
        UI_Utils.dismissTransferSuccessDialog();
        String msg = String.format(ctx.getResources().getString(R.string.destination_download_complete),
                contactsUtils.getContactNameHtml(ctx, destId));
        UI_Utils.showSnackBar(msg, Color.GREEN, Snackbar.LENGTH_LONG, false, ctx);
    }
}
