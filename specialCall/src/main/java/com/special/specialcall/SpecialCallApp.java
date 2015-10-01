package com.special.specialcall;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.services.GetTokenIntentService;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.SaveCallback;
import DataObjects.SharedConstants;
import dal_objects.DAL_Manager;
import dal_objects.SQLiteManager;
import data_objects.Constants;
import data_objects.SharedPrefUtils;
import utils.AppStateUtils;

/**
 * Created by mor on 10/09/2015.
 */
public class SpecialCallApp extends Application {

    private static final String TAG = SpecialCallApp.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        final Context context = getApplicationContext();

        // Initializing app state
        if(AppStateUtils.getAppState(context).equals(""))
            AppStateUtils.setAppState(context, TAG, SharedPrefUtils.STATE_LOGGED_OUT);

        // Initializing SQLite db
//        DAL_Manager.initialize(getApplicationContext());

        // Initializing Parse for push notifications. NOTE: Also currently using device token as registration token
        if(getDeviceToken().equals("")) {
            Parse.initialize(this, Constants.APPLICATION_ID, Constants.CLIENT_KEY);
            ParsePush.subscribeInBackground("SpecialCall");
            ParseInstallation.getCurrentInstallation().saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {

                    Intent i = new Intent(context, GetTokenIntentService.class);
                    i.setAction(GetTokenIntentService.ACTION_GET_TOKEN);
                    context.startService(i);

                }
            });
        }
    }

    private String getDeviceToken() {

        return SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN);
    }

}
