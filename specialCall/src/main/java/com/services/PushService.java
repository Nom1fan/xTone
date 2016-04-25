package com.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import com.app.AppStateManager;
import com.batch.android.Batch;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.receivers.PushReceiver;
import com.utils.BroadcastUtils;
import com.utils.MCBlockListUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.PushEventKeys;
import EventObjects.EventReport;
import EventObjects.EventType;

/**
 * Created by Mor on 05/02/2016.
 */
public class PushService extends IntentService {

    private static final String TAG = PushService.class.getSimpleName();

    public PushService() {
        super(PushService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        String jsonData;
        HashMap<DataKeys, Object> transferDetails;
        Type typeOfHashMap = new TypeToken<Map<DataKeys, Object>>() { }.getType();
        String eventActionCode;

        try {
            if (!Batch.Push.shouldDisplayPush(this, intent)) // Check that the push is valid
            {
                String errMsg = "Invalid push data! Push data was null. Terminating push receive";
                Log.e(TAG, errMsg);
                throw new Exception(errMsg);
            }

            //String alert = intent.getStringExtra(Batch.Push.ALERT_KEY);
            //BatchPushData pushData = new BatchPushData(this, intent);
            eventActionCode = intent.getStringExtra(PushEventKeys.PUSH_EVENT_ACTION);
            Log.i(TAG, "PushEventActionCode:" + eventActionCode);

            switch (eventActionCode) {
                case PushEventKeys.PENDING_DOWNLOAD: {
                    Log.i(TAG, "In:" + PushEventKeys.PENDING_DOWNLOAD);
                    jsonData = intent.getStringExtra(PushEventKeys.PUSH_EVENT_DATA);
                    transferDetails = new Gson().fromJson(jsonData, typeOfHashMap);
                    String sourceId = (String)transferDetails.get(DataKeys.SOURCE_ID);

                    if (MCBlockListUtils.IsMCBlocked(sourceId, getApplicationContext())) //don't download if the number is blocked , just break and don't continue with the download flow
                    {
                        Log.i(TAG, "NUMBER BLOCKED For DOWNLOAD: " + sourceId);
                        break;
                    }

                    Intent i = new Intent(getApplicationContext(), StorageServerProxyService.class);
                    i.setAction(StorageServerProxyService.ACTION_DOWNLOAD);
                    i.putExtra(PushEventKeys.PUSH_DATA, transferDetails);
                    startService(i);
                }
                break;

                case PushEventKeys.TRANSFER_SUCCESS: {
                    jsonData = intent.getStringExtra(PushEventKeys.PUSH_EVENT_DATA);
                    transferDetails = new Gson().fromJson(jsonData,typeOfHashMap);

                    BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.DESTINATION_DOWNLOAD_COMPLETE, null, transferDetails));
                    displayNotification(this, intent);
                }
                break;

                case PushEventKeys.CLEAR_MEDIA: {
                    jsonData = intent.getStringExtra(PushEventKeys.PUSH_EVENT_DATA);
                    transferDetails = new Gson().fromJson(jsonData, typeOfHashMap);
                    Intent i = new Intent(getApplicationContext(), ClearMediaIntentService.class);
                    i.putExtra(ClearMediaIntentService.TRANSFER_DETAILS, transferDetails);
                    startService(i);
                }
                break;

                case PushEventKeys.CLEAR_SUCCESS: {
                    jsonData = intent.getStringExtra(PushEventKeys.PUSH_EVENT_DATA);
                    transferDetails = new Gson().fromJson(jsonData, typeOfHashMap);
                    BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.CLEAR_SUCCESS, null, transferDetails));
                    displayNotification(this, intent);

                }
                break;

                case PushEventKeys.SHOW_MESSAGE: {
                    Log.i(TAG, "In:" + PushEventKeys.SHOW_MESSAGE);
                    displayNotification(this, intent);
                }
                break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            PushReceiver.completeWakefulIntent(intent);
        }
    }


    private void displayNotification(Context context, Intent intent) {

        boolean isAppInForeground = AppStateManager.isAppInForeground(context);
        String appState = AppStateManager.getAppState(context);

        Log.i(TAG, String.format("In displayNotification. [isAppInForeground]: %1$b, [App state]: %2$s", isAppInForeground, appState));

        if (isAppInForeground || appState.equals(AppStateManager.STATE_LOGGED_OUT)) {
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
            Batch.Push.displayNotification(this, intent);
    }
}

