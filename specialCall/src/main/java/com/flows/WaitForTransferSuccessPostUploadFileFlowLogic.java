package com.flows;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;

import com.app.AppStateManager;
import com.async.tasks.UploadTask;
import com.event.EventReport;
import com.event.EventType;
import com.mediacallz.app.R;
import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

/**
 * Created by Mor on 14/07/2017.
 */

public class WaitForTransferSuccessPostUploadFileFlowLogic implements PostUploadFileFlowLogic {

    public static final String TAG = WaitForTransferSuccessPostUploadFileFlowLogic.class.getSimpleName();

    @Override
    public void performPostUploadFlowLogic(UploadTask uploadTask) {
        Context context = uploadTask.getContext();
        boolean isOK = uploadTask.isOK();

        if(isOK) {
            String msg = context.getResources().getString(R.string.upload_success);
            // Setting state
            AppStateManager.setAppState(context, TAG, AppStateManager.STATE_READY);

            // Setting parameters for snackbar message
            int color = Color.GREEN;
            int sBarDuration = Snackbar.LENGTH_LONG;

            UI_Utils.showSnackBar(msg, color, sBarDuration, false, context);

            waitingForTransferSuccess(context);
        }
        else {
            BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.STORAGE_ACTION_FAILURE));
        }
    }

    private void waitingForTransferSuccess(Context context) {
        if (!SharedPrefUtils.getBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_UPLOAD_DIALOG)) {
            UI_Utils.showWaitingForTranferSuccussDialog(context, "MainActivity", context.getResources().getString(R.string.sending_to_contact)
                    , context.getResources().getString(R.string.waiting_for_transfer_sucess_dialog_msg));
        }
    }
}
