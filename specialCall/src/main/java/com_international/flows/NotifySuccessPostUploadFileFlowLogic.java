package com_international.flows;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;

import com_international.app.AppStateManager;
import com_international.async.tasks.UploadTask;
import com_international.data.objects.Constants;
import com_international.data.objects.KeysForBundle;
import com_international.enums.SpecialMediaType;
import com_international.event.EventReport;
import com_international.event.EventType;
import com_international.files.media.MediaFile;
import com_international.mediacallz.app.R;
import com_international.utils.BroadcastUtils;
import com_international.utils.LUT_Utils;
import com_international.utils.UI_Utils;

/**
 * Created by Mor on 14/07/2017.
 */

public class NotifySuccessPostUploadFileFlowLogic implements PostUploadFileFlowLogic {

    public static final String TAG = NotifySuccessPostUploadFileFlowLogic.class.getSimpleName();

    @Override
    public void performPostUploadFlowLogic(UploadTask uploadTask) {
        Context context = uploadTask.getContext();
        boolean isOK = uploadTask.isOK();
        Bundle bundle = uploadTask.getBundle();
        MediaFile mediaFile = (MediaFile) bundle.get(KeysForBundle.FILE_FOR_UPLOAD);
        SpecialMediaType specialMediaType = (SpecialMediaType) bundle.get(KeysForBundle.SPEC_MEDIA_TYPE);

        if(isOK) {
            String msg = context.getResources().getString(R.string.default_media_upload_success);
            LUT_Utils lut_utils = new LUT_Utils(specialMediaType);
            lut_utils.saveUploadedPerNumber(context, Constants.MY_ID(context), mediaFile.getFile().getAbsolutePath());

            // Setting state
            AppStateManager.setAppState(context, TAG, AppStateManager.STATE_READY);

            // Setting parameters for snackbar message
            int color = Color.GREEN;
            int sBarDuration = Snackbar.LENGTH_LONG;

            UI_Utils.showSnackBar(msg, color, sBarDuration, false, context);
        }
        else {
            BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.STORAGE_ACTION_FAILURE));
        }
    }
}
