package com.special.specialcall;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.services.GetTokenIntentService;
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



    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(this, Constants.APPLICATION_ID, Constants.CLIENT_KEY);
        ParsePush.subscribeInBackground("SpecialCall");
        ParseInstallation.getCurrentInstallation().saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {

                SharedConstants.DEVICE_TOKEN = (String) ParseInstallation.getCurrentInstallation().get("deviceToken");
                SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN, SharedConstants.DEVICE_TOKEN);

                Context context = getApplicationContext();
                Intent i = new Intent(context, GetTokenIntentService.class);
                i.setAction(GetTokenIntentService.ACTION_GET_TOKEN);
                context.startService(i);
            }
        });
    }
}
