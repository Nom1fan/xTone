package com.handlers.logic_server_proxy_service;

import android.util.Log;

import com.client.ConnectionToServer;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.model.response.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 20/12/2016.
 */
public class UpdateUserRecordActionHandler implements ActionHandler {

    private static final String TAG = UpdateUserRecordActionHandler.class.getSimpleName();
    private static final String URL_UPDATE_USER_RECORD = ROOT_URL + "/v1/UpdateUserRecord";

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();
        actionBundle.getRequest().setLocale(Locale.getDefault().getLanguage());

        log(Log.INFO, TAG, "Initiating update user record sequence...");
        int responseCode = connectionToServer.sendRequest(URL_UPDATE_USER_RECORD, actionBundle.getRequest());

        if(responseCode != HttpStatus.SC_OK) {
            log(Log.ERROR, TAG, "Insert call record failed. [Response code]:" + responseCode);
        }
    }
}
