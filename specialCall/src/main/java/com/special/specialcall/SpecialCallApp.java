package com.special.specialcall;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.SaveCallback;

import data_objects.Constants;

/**
 * Created by mor on 10/09/2015.
 */
public class SpecialCallApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(this, "7CL97UlX4EtpMyRJYshNlIQ3T12EEZ0OaZWxZjvR", "7Elu6v6XVyQRzxIqnlyIG9YGyzXuh65hD42ZUqZa");
        ParseInstallation.getCurrentInstallation().saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                Constants.DEVICE_TOKEN = (String) ParseInstallation.getCurrentInstallation().get("deviceToken");
            }
        });
    }
}
