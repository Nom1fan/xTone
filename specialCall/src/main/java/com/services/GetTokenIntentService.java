package com.services;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseInstallation;

import DataObjects.SharedConstants;
import com.utils.SharedPrefUtils;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class GetTokenIntentService extends IntentService {

    public static final String ACTION_GET_TOKEN = "com.services.action.GET_TOKEN";
    private static final int TOKEN_RETRIEVE_RETIRES = 10;
    private static final int TOKEN_RETRY_SLEEP = 1000;
    private static final String TAG = GetTokenIntentService.class.getSimpleName();

    public GetTokenIntentService() {
        super("GetTokenIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_GET_TOKEN.equals(action)) {
                handleActionGetToken();
            }
        }
    }

    /**
     * Handle action GET_TOKEN in the provided background thread with the provided
     * parameters.
     */
    private void handleActionGetToken() {

        int retries = 0;
        while ((retries < TOKEN_RETRIEVE_RETIRES) && SharedConstants.DEVICE_TOKEN == null)
        {
            retries++;
            String errMsg = "Failed to retrieve device token, retrying...";
            Log.e(TAG, errMsg);
            callToast(errMsg, Color.RED);

            SharedConstants.DEVICE_TOKEN = (String) ParseInstallation.getCurrentInstallation().get("deviceToken");
            SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN, SharedConstants.DEVICE_TOKEN);

            try {
                Thread.sleep(TOKEN_RETRY_SLEEP);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

        if (SharedConstants.DEVICE_TOKEN == null) {
            String errMsg = "Failed to retrieve device token, check your internet connection...";
            Log.e(TAG, errMsg);
            callToast(errMsg, Color.RED);
        }
        else {
            String infoMsg =  "Device token retrieved";
            Log.i(TAG, infoMsg);
            callToast(infoMsg,Color.GREEN);
        }
    }

    private void callToast(final String text, final int g) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(), text,
                        Toast.LENGTH_SHORT);
                TextView v = (TextView) toast.getView().findViewById(
                        android.R.id.message);
                v.setTextColor(g);
                toast.show();
            }
        });

    }
}
