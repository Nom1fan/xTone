package com.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import DataObjects.TransferDetails;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;

import com.utils.BroadcastUtils;
import com.utils.LUT_Utils;
import com.utils.SharedPrefUtils;
import com.utils.AppStateUtils;

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
                AppStateUtils.setAppState(context, TAG+ "STATE_IDLE", AppStateUtils.STATE_IDLE);
            break;

            case RECONNECT_ATTEMPT:
                AppStateUtils.setAppState(context, TAG+ " RECONNECT ATTEMPT", AppStateUtils.STATE_LOADING);
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, "Reconnecting...");
            break;

            case CONNECTED:
                AppStateUtils.setAppState(context, TAG+ " CONNECTED", AppStateUtils.STATE_IDLE);
            break;

            case CONNECTING:
                AppStateUtils.setAppState(context, TAG+" CONNECTING", AppStateUtils.STATE_LOADING);
                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, "Connecting...");
            break;

            case DISCONNECTED:
                AppStateUtils.setAppState(context, TAG+ " DISCONNECTED", AppStateUtils.STATE_DISABLED);
            break;

            case DESTINATION_DOWNLOAD_COMPLETE:
                TransferDetails td = (TransferDetails) report.data();
                LUT_Utils lut_utils = new LUT_Utils(context);
                lut_utils.saveUploadedPerNumber(td.getDestinationId(), td.getFileType(), td.get_fullFilePathSrcSD());

                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.REFRESH_UI, report.desc(), null));
            break;

            case LOADING_TIMEOUT:
                AppStateUtils.setAppState(context, TAG+ " LOADING_TIMEOUT", AppStateUtils.STATE_IDLE);
            break;

        }
    }

}
