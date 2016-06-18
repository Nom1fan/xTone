package com.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.app.AppStateManager;
import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.utils.BroadcastUtils;
import com.utils.CacheUtils;
import com.utils.ContactsUtils;
import com.utils.LUT_Utils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.ResponseCodes;
import DataObjects.SpecialMediaType;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;

/**
 * Created by Mor on 01/10/2015.
 */
public class BackgroundBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = BackgroundBroadcastReceiver.class.getSimpleName();
    private boolean shouldShowSnackBar = true;
    private int sBarDuration = 0;
    private int color = 0;
    private EventReport eventReport = null;
    private String msg = "";


    //TODO Mor: Need to fix the way this class sends events to MainActivity and LoginActivity. Currently very bad code.
    @Override
    public void onReceive(Context context, Intent intent) {

        EventReport report = (EventReport) intent.getSerializableExtra(Event.EVENT_REPORT);

        shouldShowSnackBar = true;

        sBarDuration = 0;
        color = 0;
        eventReport = null;
        boolean isLoading = false;
        Log.i(TAG, "Receiving event:" + report.status());

        //Ignore refresh UI event sent from here on the same events channel
        if (report.status().equals(EventType.REFRESH_UI))
            return;



        switch (report.status()) {

            //region Events for MainActivity

            //region Events in loading states
            case FETCHING_USER_DATA: {
                // Setting loading state
                String timeoutMsg = context.getResources().getString(R.string.oops_try_again);
                String loadingMsg = context.getResources().getString(R.string.fetching_user_data);
                AppStateManager.setLoadingState(context, TAG + " FETCHING_USER_DATA", loadingMsg, timeoutMsg);

                // Setting parameters for snackbar message
                msg = loadingMsg;
                color = Color.GREEN;
                sBarDuration = Snackbar.LENGTH_INDEFINITE;
                isLoading = true;
            }
            break;

            case COMPRESSING: {
                // Setting loading state
                String timeoutMsg = context.getResources().getString(R.string.compression_took_too_long);
                String loadingMsg = context.getResources().getString(R.string.compressing_file);
                AppStateManager.setLoadingState(context, TAG, loadingMsg, timeoutMsg);

                // Setting parameters for snackbar message
                msg = loadingMsg;
                color = Color.GREEN;
                sBarDuration = Snackbar.LENGTH_INDEFINITE;
                isLoading = true;
            }
            break;

            case UPLOADING: {
                // Setting loading state
                String timeoutMsg = context.getResources().getString(R.string.upload_took_too_long);
                String loadingMsg = context.getResources().getString(R.string.uploading);
                AppStateManager.setLoadingState(context, TAG, loadingMsg, timeoutMsg);

                // Setting parameters for snackbar message
                msg = loadingMsg;
                color = Color.GREEN;
                sBarDuration = Snackbar.LENGTH_INDEFINITE;
                isLoading = true;
            }
            break;

            case LOADING_CANCEL:
                AppStateManager.setAppPrevState(context, TAG + " " + EventType.LOADING_CANCEL);

                // Setting parameters for snackbar message
                msg = context.getResources().getString(R.string.action_cancelled);
                color = Color.YELLOW;
                sBarDuration = Snackbar.LENGTH_LONG;
                break;
            //endregion

            //region Events in idle, ready and disabled states
            case DISPLAY_MESSAGE:
                msg = report.desc();
                color = Color.GREEN;
                sBarDuration = Snackbar.LENGTH_INDEFINITE;
                break;

            case DISPLAY_ERROR:
                msg = report.desc();
                color = Color.RED;
                sBarDuration = Snackbar.LENGTH_INDEFINITE;
                break;

            case DESTINATION_DOWNLOAD_COMPLETE: {
                Log.i(TAG, "In: DESTINATION_DOWNLOAD_COMPLETE");
                // Preparing _data for uploaded media thumbnail display
                Map data = (Map) report.data();
                String destId = data.get(DataKeys.DESTINATION_ID).toString();
                LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.valueOf(data.get(DataKeys.SPECIAL_MEDIA_TYPE).toString()));
                lut_utils.saveUploadedPerNumber(
                        context,
                        destId,
                        FileManager.FileType.valueOf(data.get(DataKeys.FILE_TYPE).toString()),
                        data.get(DataKeys.FILE_PATH_ON_SRC_SD).toString());

                UI_Utils.dissmissTransferSuccessDialog();
                // Setting parameters for snackbar message
                msg = String.format(context.getResources().getString(R.string.destination_download_complete),
                        ContactsUtils.getContactNameHtml(context, destId));
                color = Color.GREEN;
                sBarDuration = Snackbar.LENGTH_LONG;
            }
                break;

            case CLEAR_SUCCESS: {
                // Preparing _data for uploaded media thumbnail removal
                Map data = (Map) report.data();
                String destId = data.get(DataKeys.DESTINATION_ID).toString();
                LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.valueOf(data.get(DataKeys.SPECIAL_MEDIA_TYPE).toString()));
                lut_utils.removeUploadedMediaPerNumber(context, destId);
                lut_utils.removeUploadedTonePerNumber(context, destId);

                // Setting parameters for snackbar message
                msg = String.format(context.getResources().getString(R.string.destination_media_cleared),
                        ContactsUtils.getContactNameHtml(context, destId));
                color = Color.GREEN;
                sBarDuration = Snackbar.LENGTH_LONG;
            }
                break;

            case USER_REGISTERED_TRUE: {
                String destNumber = (String) report.data();
                if(destNumber!=null) {
                    msg = String.format(context.getResources().getString(R.string.user_is_registered),
                            ContactsUtils.getContactNameHtml(context, destNumber));
                    CacheUtils.setPhone(context, (String) report.data());

                    // Setting parameters for snackbar message
                    color = Color.GREEN;
                    sBarDuration = Snackbar.LENGTH_LONG;
                }
                // Setting state
                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_READY);
            }
                break;

            case USER_REGISTERED_FALSE: {
                String destNumber = (String) report.data();
                msg =  String.format(context.getResources().getString(R.string.user_is_unregistered),
                        ContactsUtils.getContactNameHtml(context, destNumber));
            }
            case ISREGISTERED_ERROR:
            case STORAGE_ACTION_FAILURE:
            case UNREGISTER_FAILURE:
                if(msg.equals(""))
                    msg = context.getResources().getString(R.string.oops_try_again);
                // Setting app state
                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_IDLE);

                // Setting parameters for snackbar message
                color = Color.RED;
                sBarDuration = Snackbar.LENGTH_LONG;
                break;

            case UNREGISTER_SUCCESS:
                try {

                    shouldShowSnackBar = false;
                    //TODO Decide if we should delete MEDIA_CALLZ_HISTORY folder contents too or not
                    FileManager.deleteDirectoryContents(new File(Constants.INCOMING_FOLDER));
                    FileManager.deleteDirectoryContents(new File(Constants.OUTGOING_FOLDER));
                    FileManager.deleteDirectoryContents(new File(Constants.TEMP_RECORDING_FOLDER));

                    //TODO Make sure this doesn't create issues since it delete all app states and such
                    SharedPrefUtils.removeAll(context);

                    AppStateManager.setIsLoggedIn(context, false);

                    eventReport = new EventReport(EventType.REFRESH_UI);

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed during unregister procedure. [Exception]:"
                            + (e.getMessage() != null ? e.getMessage() : e));
                }
                break;
            //endregion

            //region Events in any state
            case UPDATE_USER_RECORD_SUCCESS:
                shouldShowSnackBar = false;
                Constants.MY_ANDROID_VERSION(context, Build.VERSION.RELEASE);
                break;
            //endregion

            //endregion

            //region Events for LoginActivity
            //region Events in idle
            case REGISTER_FAILURE:
                shouldShowSnackBar = false;
                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_IDLE);

                ResponseCodes resCode = (ResponseCodes) report.data();

                switch(resCode)
                {
                    case CREDENTIALS_ERR:
                        msg = context.getResources().getString(R.string.wrong_credentials);
                        break;
                    case INTERNAL_SERVER_ERR:
                        msg = context.getResources().getString(R.string.register_failure);
                        break;
                }

                eventReport = new EventReport(EventType.REFRESH_UI, msg);
                break;

            case GET_SMS_CODE_FAILURE:
                shouldShowSnackBar = false;
                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_IDLE);

                msg = context.getResources().getString(R.string.sms_code_failed);
                eventReport = new EventReport(EventType.REFRESH_UI, msg);

                break;
            //endregion

            //endregion

            //region Events for all activities
            case DISCONNECTED:
                AppStateManager.setAppState(context, TAG + " DISCONNECTED", AppStateManager.STATE_DISABLED);
                setRefreshMsgForAllActivities(context,Color.RED, context.getResources().getString(R.string.disconnected));
                sBarDuration = Snackbar.LENGTH_INDEFINITE;
                msg = ""; //TODO Mor: Disconnected event (state disabled) is handled by MainActivity separately
                break;

            case CONNECTED:
                AppStateManager.setAppState(context, TAG, AppStateManager.getAppPrevState(context));
                setRefreshMsgForAllActivities(context, Color.GREEN, context.getResources().getString(R.string.connected));
                break;

            case LOADING_TIMEOUT:
                AppStateManager.setAppState(context, TAG, AppStateManager.getAppPrevState(context));
                String timeoutMsg = AppStateManager.getTimeoutMsg(context);
                setRefreshMsgForAllActivities(context, Color.RED, timeoutMsg);
                break;
            //endregion

            default: // Event not meant for background receiver
                    return;

        }

        if(shouldShowSnackBar)
            UI_Utils.showSnackBar(msg, color, sBarDuration, isLoading, context);
        if(eventReport!=null)
            BroadcastUtils.sendEventReportBroadcast(context, TAG, eventReport);

    }

    private void setRefreshMsgForAllActivities(Context context, int color, String msg) {

        this.msg = msg;
        if(AppStateManager.isLoggedIn(context)) {

            this.color = color;
            sBarDuration = Snackbar.LENGTH_LONG;
        }
        else {
            shouldShowSnackBar = false;
            eventReport = new EventReport(EventType.REFRESH_UI, msg);
        }
    }

}
