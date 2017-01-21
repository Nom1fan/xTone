package com.handlers.logic_server_proxy_service;

import android.util.Log;

import com.client.ConnectionToServer;
import com.data.objects.MediaCall;
import com.handlers.ActionHandler;
import com.model.request.InsertMediaCallRecordRequest;

import java.io.IOException;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;
import static com.services.ServerProxyService.MEDIA_CALL;

/**
 * Created by Mor on 20/12/2016.
 */
public class InsertCallRecordActionHandler implements ActionHandler {

    private static final String TAG = InsertCallRecordActionHandler.class.getSimpleName();
    private static final String URL_INSERT_CALL_RECORD = ROOT_URL + "/v1/InsertMediaCallRecord";

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        MediaCall mediaCall = (MediaCall) actionBundle.getIntent().getSerializableExtra(MEDIA_CALL);
        InsertMediaCallRecordRequest request = new InsertMediaCallRecordRequest(actionBundle.getRequest());
        request.setMediaCall(mediaCall);
        request.setSourceLocale(Locale.getDefault().getLanguage());
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();

        log(Log.INFO, TAG, "Initiating insert call record sequence...");
        int responseCode = connectionToServer.sendRequest(URL_INSERT_CALL_RECORD, request);
        connectionToServer.readResponse();

        if (responseCode != HttpStatus.SC_OK) {
            log(Log.ERROR, TAG, "Insert call record failed. [Response code]:" + responseCode);
        }
    }
}
