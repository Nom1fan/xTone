package com.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import DataObjects.TransferDetails;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;

import com.app.AppStateManager;
import com.utils.BroadcastUtils;
import com.utils.LUT_Utils;
import com.utils.SharedPrefUtils;

/**
 * Created by Mor on 01/10/2015.
 */
public class AppStateBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = AppStateBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        EventReport report = (EventReport) intent.getSerializableExtra(Event.EVENT_REPORT);

        switch(report.status())
        {
            case REGISTER_SUCCESS:
                AppStateManager.setAppState(context, TAG + "STATE_IDLE", AppStateManager.STATE_IDLE);
            break;

            case RECONNECT_ATTEMPT:
                AppStateManager.setAppState(context, TAG + " RECONNECT ATTEMPT", AppStateManager.STATE_LOADING);
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, "Reconnecting...");
            break;

            case CONNECTED:
                AppStateManager.setAppState(context, TAG + " CONNECTED", AppStateManager.STATE_IDLE);
            break;

            case CONNECTING:
                AppStateManager.setAppState(context, TAG + " CONNECTING", AppStateManager.STATE_LOADING);
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, "Connecting...");
            break;

            case DISCONNECTED:
                AppStateManager.setAppState(context, TAG + " DISCONNECTED", AppStateManager.STATE_DISABLED);
            break;

            case DESTINATION_DOWNLOAD_COMPLETE:
                TransferDetails td = (TransferDetails) report.data();
                LUT_Utils lut_utils = new LUT_Utils(context);
                lut_utils.saveUploadedPerNumber(td.getDestinationId(), td.getFileType(), td.get_fullFilePathSrcSD());

                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.REFRESH_UI, report.desc(), null));
            break;

            case LOADING_TIMEOUT:
                AppStateManager.setAppState(context, TAG + " LOADING_TIMEOUT", AppStateManager.STATE_IDLE);
            break;

        }
    }

}
