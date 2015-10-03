package com.special.specialcall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import EventObjects.Event;
import EventObjects.EventReport;
import data_objects.SharedPrefUtils;
import utils.AppStateUtils;

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
            case LOGIN_SUCCESS:
                AppStateUtils.setAppState(context, TAG, SharedPrefUtils.STATE_IDLE);
            break;

            case RECONNECT_ATTEMPT:
                AppStateUtils.setAppState(context, TAG, SharedPrefUtils.STATE_LOADING);
            break;

            case DISCONNECTED:
                AppStateUtils.setAppState(context, TAG, SharedPrefUtils.STATE_DISABLED);
            break;

        }
    }
}
