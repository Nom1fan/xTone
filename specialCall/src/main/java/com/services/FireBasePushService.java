package com.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.batch.android.Batch;
import com.data.objects.PushEventKeys;
import com.exceptions.PushDataEmptyException;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.handlers.HandlerFactory;
import com.handlers.PushHandler;
import com.receivers.PushReceiver;

import java.util.Map;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 23/12/2017.
 */
public class FireBasePushService extends FirebaseMessagingService {

    private static final String TAG = FireBasePushService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        Map<String, String> pushData = remoteMessage.getData();

        if (pushData.size() == 0 && remoteMessage.getNotification() == null) {
            throw new PushDataEmptyException("Push had no data nor notification message");
        }

        Log.d(TAG, "Message pushData payload: " + pushData);
        String eventActionCode = pushData.get(PushEventKeys.PUSH_EVENT_ACTION);

        log(Log.INFO, TAG, "PushEventActionCode:" + eventActionCode);

        PushHandler pushHandler = HandlerFactory.getInstance().getPushHandler(eventActionCode);
        String jsonData = pushData.get(PushEventKeys.PUSH_EVENT_DATA);
        pushHandler.handlePush(getApplicationContext(), jsonData, remoteMessage);
    }
}

