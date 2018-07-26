package com.xtone.service;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;

import java.util.Map;

/**
 * Created by Mor on 23/12/2017.
 */
public class FireBasePushService extends FirebaseMessagingService {

    private static final Logger log = LoggerFactory.getLogger();

    private static final String TAG = FireBasePushService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        Map<String, String> pushData = remoteMessage.getData();

        if (pushData.size() == 0 && remoteMessage.getNotification() == null) {
            throw new RuntimeException("Push had no data nor notification message");
        }

        log.debug(TAG, String.format("Message pushData payload:[%s]", pushData));

        //handle push
    }
}

