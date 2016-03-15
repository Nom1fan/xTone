package com.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.batch.android.Batch;
import com.data_objects.Constants;


import EventObjects.EventReport;
import EventObjects.EventType;

import com.utils.BroadcastUtils;


public class GetTokenIntentService extends IntentService {

    public static final String ACTION_GET_BATCH_TOKEN = "com.services.action.GET_BATCH_TOKEN";

    private static final int TOKEN_RETRIEVE_RETRIES = 10;
    private static final int TOKEN_RETRY_SLEEP = 1000;
    private static final String TAG = GetTokenIntentService.class.getSimpleName();

    public GetTokenIntentService() {
        super("GetTokenIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (ACTION_GET_BATCH_TOKEN.equals(action)) {
                handleActionGetBatchToken();
            }
        }
    }


    /**
     * Handle action GET_BATCH_TOKEN in the provided background thread with the provided
     * parameters.
     */
    private void handleActionGetBatchToken() {

        Context context = getApplicationContext();

        try {

            String token = "";
            int retries = 0;
            do {
                retries++;
                String infoMsg = "Attempt " + retries + "/" + TOKEN_RETRIEVE_RETRIES + " to retrieve batch token";
                Log.i(TAG, infoMsg);

                //token = Batch.Push.getLastKnownPushToken();
                token = Batch.User.getInstallationID();
                Constants.MY_BATCH_TOKEN(context, token);

                try {
                    Thread.sleep(TOKEN_RETRY_SLEEP);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            while ((retries < TOKEN_RETRIEVE_RETRIES) && Constants.MY_BATCH_TOKEN(context).equals(""));

            if (Constants.MY_BATCH_TOKEN(context).equals("")) {
                String errMsg = "Oops! \n Check your connection and restart MediaCallz...";
                Log.e(TAG, errMsg);
                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.TOKEN_RETRIEVAL_FAILED, errMsg, null));
            } else {
                String infoMsg = "Device batch token retrieved";
                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.TOKEN_RETRIEVED, null, null));
                Log.i(TAG, infoMsg + ":" + token);
                //callToast(infoMsg,Color.GREEN);
            }
        } catch (Exception e) {
            Log.e(TAG, "Trying to retrieve token failed:" + (e.getMessage() != null ? e.getMessage() : e));
        } finally {

            context = null;
        }
    }
}
