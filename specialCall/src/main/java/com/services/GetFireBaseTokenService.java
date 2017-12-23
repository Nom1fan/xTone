package com.services;

import android.content.Context;
import android.util.Log;

import com.data.objects.Constants;
import com.event.EventReport;
import com.event.EventType;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.utils.BroadcastUtils;

import org.apache.commons.lang.StringUtils;

import static com.crashlytics.android.Crashlytics.log;


public class GetFireBaseTokenService extends FirebaseInstanceIdService {

    private static final String TAG = GetFireBaseTokenService.class.getSimpleName();
    private static final int TOKEN_RETRIEVE_RETRIES = 10;
    private static final int TOKEN_RETRY_SLEEP = 1000;

    @Override
    public void onTokenRefresh() {
        attemptToGetTokenIteratively();
    }

    private void attemptToGetTokenIteratively() {

        Context context = getApplicationContext();

        String token = FirebaseInstanceId.getInstance().getToken();
        try {

            int retries = 0;
            do {
                retries++;
                String infoMsg = "Attempt " + (retries + 1) + "/" + TOKEN_RETRIEVE_RETRIES + " to retrieve firebase token";
                log(Log.INFO, TAG, infoMsg);

                Thread.sleep(TOKEN_RETRY_SLEEP);

            } while ((retries < TOKEN_RETRIEVE_RETRIES) && StringUtils.isEmpty(token));


            if (StringUtils.isEmpty(token)) {
                String errMsg = "Oops! \n Check your connection and restart MediaCallz...";
                log(Log.ERROR, TAG, errMsg);
                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.TOKEN_RETRIEVAL_FAILED, errMsg));
            } else {
                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.TOKEN_RETRIEVED));
                log(Log.INFO, TAG, "Device firebase token retrieved:" + token);
                Constants.MY_FIREBASE_TOKEN(context, token);
            }
        } catch (Exception e) {
            log(Log.ERROR, TAG, "Trying to retrieve token failed:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

}
