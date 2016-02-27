package com.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

import com.app.AppStateManager;
import com.data_objects.SnackbarData;
import com.nispok.snackbar.Snackbar;
import com.utils.BroadcastUtils;
import com.utils.CacheUtils;
import com.utils.LUT_Utils;
import com.utils.SharedPrefUtils;

import DataObjects.TransferDetails;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;

/**
 * Created by Mor on 01/10/2015.
 */
public class BackgroundBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = BackgroundBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        EventReport report = (EventReport) intent.getSerializableExtra(Event.EVENT_REPORT);

        //Ignore refresh UI event sent from here on the same events channel
        if (report.status().equals(EventType.REFRESH_UI))
            return;

        SnackbarData snackbarData = null;
        Snackbar.SnackbarDuration sBarDuration = null;
        int color = 0;

        switch (report.status()) {
            //region Events in loading states
            case RECONNECT_ATTEMPT: {
                // Setting loading state
                String timeOutMsg = "Oops! Please check your internet connection.";
                AppStateManager.setAppState(context, TAG + " RECONNECT ATTEMPT",
                        AppStateManager.createLoadingState(new EventReport(EventType.DISCONNECTED, timeOutMsg, null), 0));
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, report.desc());

                // Setting parameters for snackbar message
                color = Color.RED;
                sBarDuration = Snackbar.SnackbarDuration.LENGTH_INDEFINITE;
            }
            break;

            case FETCHING_USER_DATA: {
                // Setting loading state
                String msg = "Oops! Please try again.";
                AppStateManager.setAppState(context, TAG + " FETCHING_USER_DATA",
                        AppStateManager.createLoadingState(new EventReport(EventType.ISREGISTERED_ERROR, msg, null), 10 * 1000));
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, report.desc());

                // Setting parameters for snackbar message
                color = Color.GREEN;
                sBarDuration = Snackbar.SnackbarDuration.LENGTH_LONG;
            }
            break;

            case CONNECTING: {
                // Setting loading state
                String msg = "Oops! Please check your internet connection.";
                AppStateManager.setAppState(context, TAG + " CONNECTING",
                        AppStateManager.createLoadingState(new EventReport(EventType.DISCONNECTED, msg, null), 0));
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, report.desc());

                // Setting parameters for snackbar message
                color = Color.GREEN;
                sBarDuration = Snackbar.SnackbarDuration.LENGTH_INDEFINITE;
            }
            break;

            case COMPRESSING: {
                // Setting loading state
                String timeoutMsg = "Oops! Compression took too long!";
                AppStateManager.setAppState(context, TAG,
                        AppStateManager.createLoadingState(new EventReport(EventType.REFRESH_UI, timeoutMsg, null), 15 * 1000));
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, report.desc());

                // Setting parameters for snackbar message
                color = Color.GREEN;
                sBarDuration = Snackbar.SnackbarDuration.LENGTH_INDEFINITE;
            }
            break;

            case UPLOADING: {
                // Setting loading state
                String timeoutMsg = "Oops! upload took too long!";
                AppStateManager.setAppState(context, TAG,
                        AppStateManager.createLoadingState(new EventReport(EventType.REFRESH_UI, timeoutMsg, null), 15 * 1000));
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, report.desc());

                // Setting parameters for snackbar message
                color = Color.GREEN;
                sBarDuration = Snackbar.SnackbarDuration.LENGTH_INDEFINITE;
            }
            break;
            //endregion

            //region Events in Idle, ready and disabled states
            case DISCONNECTED:
                // Setting state
                AppStateManager.setAppState(context, TAG + " DISCONNECTED", AppStateManager.STATE_DISABLED);

                // Setting parameters for snackbar message
                color = Color.RED;
                sBarDuration = Snackbar.SnackbarDuration.LENGTH_INDEFINITE;
                break;

            case DESTINATION_DOWNLOAD_COMPLETE: {
                Log.i(TAG, "In: DESTINATION_DOWNLOAD_COMPLETE");
                // Preparing data for uploaded media thumbnail display
                TransferDetails td = (TransferDetails) report.data();
                LUT_Utils lut_utils = new LUT_Utils(td.get_spMediaType());
                lut_utils.saveUploadedPerNumber(context, td.getDestinationId(), td.getFileType(), td.get_fullFilePathSrcSD());

                // Setting parameters for snackbar message
                color = Color.GREEN;
                sBarDuration = Snackbar.SnackbarDuration.LENGTH_LONG;
            }
                break;

            case CLEAR_SUCCESS: {
                // Preparing data for uploaded media thumbnail removal
                TransferDetails td = (TransferDetails) report.data();
                LUT_Utils lut_utils = new LUT_Utils(td.get_spMediaType());
                lut_utils.removeUploadedMediaPerNumber(context, td.getDestinationId());
                lut_utils.removeUploadedTonePerNumber(context, td.getDestinationId());

                // Setting parameters for snackbar message
                color = Color.GREEN;
                sBarDuration = Snackbar.SnackbarDuration.LENGTH_LONG;
            }
                break;

            case USER_REGISTERED_TRUE:
                CacheUtils.setPhone(context, (String) report.data());
            case UPLOAD_SUCCESS:
            case COMPRESSION_COMPLETE:
                // Setting state
                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_READY);

                // Setting parameters for snackbar message
                color = Color.GREEN;
                sBarDuration = Snackbar.SnackbarDuration.LENGTH_LONG;
                break;

            case CONNECTED:
                // Setting state based on previous state
                String prevState = AppStateManager.getAppPrevState(context);
                if (prevState.equals(AppStateManager.STATE_LOGGED_IN))
                    AppStateManager.setAppState(context, TAG, AppStateManager.STATE_IDLE);
                else
                    AppStateManager.setAppState(context, TAG, AppStateManager.getAppPrevState(context));

                // Setting parameters for snackbar message
                color = Color.GREEN;
                sBarDuration = Snackbar.SnackbarDuration.LENGTH_LONG;
                break;

            case REGISTER_SUCCESS:
                // Setting state
                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_IDLE);

                // Setting parameters for snackbar message
                color = Color.GREEN;
                sBarDuration = Snackbar.SnackbarDuration.LENGTH_LONG;
                break;

            case USER_REGISTERED_FALSE:
            case ISREGISTERED_ERROR:
            case UPLOAD_FAILURE:
                // Setting app state
                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_IDLE);

                // Setting parameters for snackbar message
                color = Color.RED;
                sBarDuration = Snackbar.SnackbarDuration.LENGTH_LONG;
                break;

            //TODO Mor: Implement UNREGISTER_SUCCESS and UNREGISTER_FAILURE events handling
            case UNREGISTER_SUCCESS:
                break;

            case UNREGISTER_FAILURE:
                break;
            //endregion

            default: // Event not meant for background receiver
                    return;

        }

        snackbarData = new SnackbarData(SnackbarData.SnackbarStatus.SHOW, color, sBarDuration, report.desc());
        BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.REFRESH_UI, report.desc(), snackbarData));
    }

}
