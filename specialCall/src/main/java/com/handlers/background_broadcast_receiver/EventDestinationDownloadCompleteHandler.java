package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.handlers.Handler;
import com.mediacallz.app.R;
import com.utils.ContactsUtils;
import com.utils.LUT_Utils;
import com.utils.UI_Utils;

import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.SpecialMediaType;
import EventObjects.EventReport;
import FilesManager.FileManager;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventDestinationDownloadCompleteHandler implements Handler {

    private static final String TAG = EventDestinationDownloadCompleteHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        EventReport eventReport = (EventReport) params[0];

        log(Log.INFO,TAG, "In: DESTINATION_DOWNLOAD_COMPLETE");
        // Preparing _data for uploaded media thumbnail display
        Map data = (Map) eventReport.data();
        String destId = data.get(DataKeys.DESTINATION_ID).toString();
        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.valueOf(data.get(DataKeys.SPECIAL_MEDIA_TYPE).toString()));
        lut_utils.saveUploadedPerNumber(
                ctx,
                destId,
                FileManager.FileType.valueOf(data.get(DataKeys.FILE_TYPE).toString()),
                data.get(DataKeys.FILE_PATH_ON_SRC_SD).toString());

        UI_Utils.dismissTransferSuccessDialog();
        String msg = String.format(ctx.getResources().getString(R.string.destination_download_complete),
                ContactsUtils.getContactNameHtml(ctx, destId));
        UI_Utils.showSnackBar(msg, Color.GREEN, Snackbar.LENGTH_LONG, false, ctx);
    }
}
