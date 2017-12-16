package com_international.handlers.server_proxy_service;

import android.util.Log;

import com_international.client.ConnectionToServerImpl;
import com_international.handlers.ActionHandler;

import java.io.IOException;
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
        ConnectionToServerImpl connectionToServer = actionBundle.getConnectionToServer();
        actionBundle.getRequest().setLocale(Locale.getDefault().getLanguage());

        log(Log.INFO, TAG, "Initiating update user record sequence...");
        int responseCode = connectionToServer.sendRequest(URL_UPDATE_USER_RECORD, actionBundle.getRequest());

        if(responseCode != HttpStatus.SC_OK) {
            log(Log.ERROR, TAG, "Insert call record failed. [Response code]:" + responseCode);
        }
    }
}
