package com.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import DataObjects.SharedConstants;
import DataObjects.TransferDetails;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;

import com.app.AppStateManager;
import com.data_objects.SnackbarData;
import com.nispok.snackbar.Snackbar;
import com.utils.BroadcastUtils;
import com.utils.LUT_Utils;
import com.utils.SharedPrefUtils;

/**
 * Created by Mor on 01/10/2015.
 */
public class BackgroundBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = BackgroundBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        EventReport report = (EventReport) intent.getSerializableExtra(Event.EVENT_REPORT);
        SnackbarData snackbarData = null;

        switch(report.status())
        {
            /* Events in loading states */

            case RECONNECT_ATTEMPT: {
                String msg = "Oops! Please check your internet connection.";
                AppStateManager.setAppState(context, TAG + " RECONNECT ATTEMPT",
                        AppStateManager.createLoadingState(new EventReport(EventType.DISCONNECTED, msg ,null), 0));
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, report.desc());
                snackbarData = new SnackbarData(
                        SnackbarData.SnackbarStatus.SHOW,
                        Color.RED, Snackbar.SnackbarDuration.LENGTH_INDEFINITE,
                        report.desc());
            }
            break;

            case FETCHING_USER_DATA: {
                String msg = "Oops! Please try again.";
                AppStateManager.setAppState(context, TAG + " FETCHING_USER_DATA",
                        AppStateManager.createLoadingState(new EventReport(EventType.ISREGISTERED_ERROR, msg, null), 10*1000));
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, report.desc());
                snackbarData = new SnackbarData(
                        SnackbarData.SnackbarStatus.SHOW,
                        Color.GREEN, Snackbar.SnackbarDuration.LENGTH_LONG,
                        report.desc());
            }
            break;

            case CONNECTING: {
                String msg = "Oops! Please check your internet connection.";
                AppStateManager.setAppState(context, TAG + " CONNECTING",
                        AppStateManager.createLoadingState(new EventReport(EventType.DISCONNECTED, msg, null), 0));
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, report.desc());
                snackbarData = new SnackbarData(
                        SnackbarData.SnackbarStatus.SHOW,
                        Color.GREEN, Snackbar.SnackbarDuration.LENGTH_INDEFINITE,
                        report.desc());
            }
            break;

            case COMPRESSING: {
                String timeoutMsg = "Oops! Compression took too long!";
                AppStateManager.setAppState(context, TAG,
                        AppStateManager.createLoadingState(new EventReport(EventType.REFRESH_UI, timeoutMsg, null), 15*1000));
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, report.desc());
                snackbarData = new SnackbarData(
                        SnackbarData.SnackbarStatus.SHOW,
                        Color.GREEN, Snackbar.SnackbarDuration.LENGTH_INDEFINITE,
                        report.desc());
            }
            break;

            case UPLOADING: {
                String timeoutMsg = "Oops! upload took too long!";
                AppStateManager.setAppState(context, TAG,
                        AppStateManager.createLoadingState(new EventReport(EventType.REFRESH_UI, timeoutMsg, null), 15*1000));
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, report.desc());
                snackbarData = new SnackbarData(
                        SnackbarData.SnackbarStatus.SHOW,
                        Color.GREEN, Snackbar.SnackbarDuration.LENGTH_INDEFINITE,
                        report.desc());
            }
            break;

            /* Events in Idle, ready and disabled states */

            case DISCONNECTED:
                AppStateManager.setAppState(context, TAG + " DISCONNECTED", AppStateManager.STATE_DISABLED);
                snackbarData = new SnackbarData(
                        SnackbarData.SnackbarStatus.SHOW,
                        Color.RED, Snackbar.SnackbarDuration.LENGTH_INDEFINITE,
                        report.desc());
            break;

            case DESTINATION_DOWNLOAD_COMPLETE:
                TransferDetails td = (TransferDetails) report.data();
                LUT_Utils lut_utils = new LUT_Utils(context, td.get_spMediaType());
                lut_utils.saveUploadedPerNumber(td.getDestinationId(), td.getFileType(), td.get_fullFilePathSrcSD());
                lut_utils.destroy();
                snackbarData = new SnackbarData(
                        SnackbarData.SnackbarStatus.SHOW,
                        Color.GREEN, Snackbar.SnackbarDuration.LENGTH_LONG,
                        report.desc());
            break;

            case USER_REGISTERED_TRUE:
            case UPLOAD_SUCCESS:
            case COMPRESSION_COMPLETE:
                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_READY);
                snackbarData = new SnackbarData(
                        SnackbarData.SnackbarStatus.SHOW,
                        Color.GREEN, Snackbar.SnackbarDuration.LENGTH_LONG,
                        report.desc());
                break;

            case CONNECTED:
                String prevState = AppStateManager.getAppPrevState(context);
                if(prevState.equals(AppStateManager.STATE_LOGGED_IN))
                    AppStateManager.setAppState(context, TAG, AppStateManager.STATE_IDLE);
                else
                    AppStateManager.setAppState(context, TAG, AppStateManager.getAppPrevState(context));

                snackbarData = new SnackbarData(
                        SnackbarData.SnackbarStatus.SHOW,
                        Color.GREEN, Snackbar.SnackbarDuration.LENGTH_LONG,
                        report.desc());
                break;

            case REGISTER_SUCCESS:
                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_IDLE);
                snackbarData = new SnackbarData(
                        SnackbarData.SnackbarStatus.SHOW,
                        Color.GREEN, Snackbar.SnackbarDuration.LENGTH_LONG,
                        report.desc());
                break;

            case ISREGISTERED_ERROR:
            case UPLOAD_FAILURE:
            case USER_REGISTERED_FALSE:
                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_IDLE);

                snackbarData = new SnackbarData(
                        SnackbarData.SnackbarStatus.SHOW,
                        Color.RED, Snackbar.SnackbarDuration.LENGTH_LONG,
                        report.desc());
                break;

            //TODO Mor: Implement UNREGISTER_SUCCESS and UNREGISTER_FAILURE events handling
            case UNREGISTER_SUCCESS:
                break;

            case UNREGISTER_FAILURE:
                break;
        }

        if(!report.status().equals(EventType.REFRESH_UI))
            BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.REFRESH_UI, report.desc(), snackbarData));
    }

}
