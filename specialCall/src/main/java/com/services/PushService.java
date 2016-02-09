package com.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.batch.android.Batch;
import com.google.gson.Gson;
import com.receivers.PushReceiver;
import com.utils.BroadcastUtils;

import DataObjects.PushEventKeys;
import DataObjects.TransferDetails;
import EventObjects.EventReport;
import EventObjects.EventType;

/**
 * Created by Mor on 05/02/2016.
 */
public class PushService extends IntentService {

    private static final String TAG = PushService.class.getSimpleName();
    private Context _context;
    private Gson gson = new Gson();

    public PushService() {
        super(PushService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        _context = getApplicationContext();

        String jsonData;
        TransferDetails td;
        String eventActionCode;

        try {
            if( !Batch.Push.shouldDisplayPush(this, intent) ) // Check that the push is valid
            {
                String errMsg = "Invalid push data! Push data was null. Terminating push receive";
                Log.e(TAG, errMsg);
                throw new Exception(errMsg);
            }

            //String alert = intent.getStringExtra(Batch.Push.ALERT_KEY);
            //BatchPushData pushData = new BatchPushData(this, intent);
            eventActionCode = intent.getStringExtra(PushEventKeys.PUSH_EVENT_ACTION);
            Log.i(TAG, "PushEventActionCode:" + eventActionCode);

            switch(eventActionCode)
            {
                case PushEventKeys.PENDING_DOWNLOAD:
                    Log.i(TAG, "In:" + PushEventKeys.PENDING_DOWNLOAD);
                    jsonData = intent.getStringExtra(PushEventKeys.PUSH_EVENT_DATA);
                    td = gson.fromJson(jsonData, TransferDetails.class);

                    Intent i = new Intent(_context.getApplicationContext(), StorageServerProxyService.class);
                    i.setAction(StorageServerProxyService.ACTION_DOWNLOAD);
                    i.putExtra(PushEventKeys.PUSH_DATA, td);
                    _context.startService(i);
                    break;

                case PushEventKeys.TRANSFER_SUCCESS:
                    String msg = intent.getStringExtra(Batch.Push.ALERT_KEY);
                    jsonData = intent.getStringExtra(PushEventKeys.PUSH_EVENT_DATA);
                    td = gson.fromJson(jsonData, TransferDetails.class);

                    BroadcastUtils.sendEventReportBroadcast(_context, TAG, new EventReport(EventType.DESTINATION_DOWNLOAD_COMPLETE, msg, td));
                    Batch.Push.displayNotification(this, intent);
                    break;

                case PushEventKeys.SHOW_MESSAGE:
                    Log.i(TAG, "In:" + PushEventKeys.SHOW_MESSAGE);
                    Batch.Push.displayNotification(this, intent);
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            PushReceiver.completeWakefulIntent(intent);
        }
    }

}

