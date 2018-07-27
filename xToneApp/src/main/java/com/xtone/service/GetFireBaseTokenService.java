package com.xtone.service;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

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

        String token = FirebaseInstanceId.getInstance().getToken();
        try {

            int retries = 0;
            do {
                retries++;
                String infoMsg = "Attempt " + retries + "/" + TOKEN_RETRIEVE_RETRIES + " to retrieve firebase token";
                log(Log.INFO, TAG, infoMsg);

                Thread.sleep(TOKEN_RETRY_SLEEP);

            } while ((retries < TOKEN_RETRIEVE_RETRIES) && StringUtils.isEmpty(token));


            if (StringUtils.isEmpty(token)) {
                String errMsg = "Oops! \n Check your connection and restart MediaCallz...";
                log(Log.ERROR, TAG, errMsg);

            } else {

                log(Log.INFO, TAG, "Device firebase token retrieved:" + token);

            }
        } catch (Exception e) {
            log(Log.ERROR, TAG, "Trying to retrieve token failed:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

}
