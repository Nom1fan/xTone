package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.data.objects.PendingDownloadData;
import com.event.EventReport;
import com.handlers.Handler;
import com.mediacallz.app.R;
import com.utils.ContactsUtils;
import com.utils.UI_Utils;
import com.utils.UtilityFactory;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventDownloadFailureHandler implements Handler {

    private static final String TAG = EventDownloadFailureHandler.class.getSimpleName();

    private final ContactsUtils contactsUtils = UtilityFactory.instance().getUtility(ContactsUtils.class);


    @Override
    public void handle(Context ctx, Object... params) {
        EventReport eventReport = (EventReport) params[0];

        log(Log.INFO, TAG, "Handling download failure event");
        // Preparing _data for uploaded media thumbnail display
        PendingDownloadData data = (PendingDownloadData) eventReport.data();
        String destId = data.getDestinationId();

        UI_Utils.dismissWaitingForTransferSuccessDialog();
        String msg = String.format(ctx.getResources().getString(R.string.destination_download_failed),
                contactsUtils.getContactNameHtml(ctx, destId));
        UI_Utils.showSnackBar(msg, Color.RED, Snackbar.LENGTH_LONG, false, ctx);
    }
}
