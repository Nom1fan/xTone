package com.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.batch.android.Batch;
import com.handlers.HandlerFactory;
import com.handlers.PushHandler;
import com.receivers.PushReceiver;

import com.data.objects.PushEventKeys;

import static com.crashlytics.android.Crashlytics.log;

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

        try {
            if (!Batch.Push.shouldDisplayPush(this, intent)) // Check that the push is valid
            {
                String errMsg = "Invalid push data! Push data was null. Terminating push receive";
                log(Log.ERROR,TAG, errMsg);
                throw new Exception(errMsg);
            }

//            String alert = intent.getStringExtra(Batch.Push.ALERT_KEY);
            //BatchPushData pushData = new BatchPushData(this, intent);

            String eventActionCode = intent.getStringExtra(PushEventKeys.PUSH_EVENT_ACTION);
            log(Log.INFO,TAG, "PushEventActionCode:" + eventActionCode);

            PushHandler pushHandler = HandlerFactory.getInstance().getPushHandler(eventActionCode);
            String jsonData = intent.getStringExtra(PushEventKeys.PUSH_EVENT_DATA);
            pushHandler.handlePush(getApplicationContext(), jsonData, intent);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            PushReceiver.completeWakefulIntent(intent);
        }
    }
}

