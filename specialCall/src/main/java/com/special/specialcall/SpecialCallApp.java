package com.special.specialcall;

import android.app.Application;
import android.graphics.Color;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.SaveCallback;

import DataObjects.SharedConstants;
import data_objects.Constants;
import data_objects.SharedPrefUtils;

/**
 * Created by mor on 10/09/2015.
 */
public class SpecialCallApp extends Application {

    private static final int TOKEN_RETRIEVE_RETIRES = 10;
    private static final int TOKEN_RETRY_SLEEP = 1000;

    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(this, Constants.APPLICATION_ID, Constants.CLIENT_KEY);
        ParsePush.subscribeInBackground("SpecialCall");
        ParseInstallation.getCurrentInstallation().saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {

                int retries = 0;
                do
                {
                    SharedConstants.DEVICE_TOKEN = (String) ParseInstallation.getCurrentInstallation().get("deviceToken");
                    SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN, SharedConstants.DEVICE_TOKEN);
                }
                while((retries < TOKEN_RETRIEVE_RETIRES) && SharedConstants.DEVICE_TOKEN==null);
                {
                    retries++;
                    callToast("Failed to retrieve device token, retrying...", Color.RED);
                    SharedConstants.DEVICE_TOKEN = (String) ParseInstallation.getCurrentInstallation().get("deviceToken");
                    SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN, SharedConstants.DEVICE_TOKEN);
                    try {
                        Thread.sleep(TOKEN_RETRY_SLEEP);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }

                if(retries==TOKEN_RETRIEVE_RETIRES)
                    callToast("Failed to retrieve device token, check your internet connection...", Color.RED);
                else
                    callToast("Device token retrieved", Color.GREEN);

            }
        });
    }

    private void callToast(final String text, final int g) {

        Toast toast = Toast.makeText(getApplicationContext(), text,
                Toast.LENGTH_LONG);
        TextView v = (TextView) toast.getView().findViewById(
                android.R.id.message);
        v.setTextColor(g);
        toast.show();


    }
}
