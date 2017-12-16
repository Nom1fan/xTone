package com_international.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.batch.android.Batch;
import com_international.data.objects.Constants;
import com_international.utils.BroadcastUtils;

import com_international.event.EventReport;
import com_international.event.EventType;

import static com.crashlytics.android.Crashlytics.log;


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
                log(Log.INFO,TAG, infoMsg);

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
                log(Log.ERROR,TAG, errMsg);
                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.TOKEN_RETRIEVAL_FAILED, errMsg, null));
            } else {
                String infoMsg = "Device batch token retrieved";
                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.TOKEN_RETRIEVED, null, null));
                log(Log.INFO,TAG, infoMsg + ":" + token);
                //callToast(infoMsg,Color.GREEN);
            }
        } catch (Exception e) {
            log(Log.ERROR,TAG, "Trying to retrieve token failed:" + (e.getMessage() != null ? e.getMessage() : e));
        } finally {

            context = null;
        }
    }
}
