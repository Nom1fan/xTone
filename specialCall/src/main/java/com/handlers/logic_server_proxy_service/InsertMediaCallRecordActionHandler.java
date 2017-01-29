package com.handlers.logic_server_proxy_service;

import android.util.Log;

import com.client.ConnectionToServer;
import com.data.objects.AppMeta;
import com.data.objects.MediaCall;
import com.event.EventReport;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.model.request.InsertMediaCallRecordRequest;
import com.model.response.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;
import static com.services.ServerProxyService.MEDIA_CALL;

/**
 * Created by Mor on 20/12/2016.
 */
public class InsertMediaCallRecordActionHandler implements ActionHandler {

    private static final String TAG = InsertMediaCallRecordActionHandler.class.getSimpleName();
    private static final String URL_INSERT_CALL_RECORD = ROOT_URL + "/v1/InsertMediaCallRecord";
    private static final Type responseType = new TypeToken<Response<Integer>>() {
    }.getType();

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        MediaCall mediaCall = (MediaCall) actionBundle.getIntent().getSerializableExtra(MEDIA_CALL);
        InsertMediaCallRecordRequest request = new InsertMediaCallRecordRequest(actionBundle.getRequest());
        request.setMediaCall(mediaCall);
        request.setLocale(Locale.getDefault().getLanguage());
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();
        connectionToServer.setResponseType(responseType);

        log(Log.INFO, TAG, "Initiating insert call record sequence...");
        int responseCode = connectionToServer.sendRequest(URL_INSERT_CALL_RECORD, request);
        if (responseCode == HttpStatus.SC_OK) {
            Response<Integer> response = connectionToServer.readResponse();
            int callId = response.getResult();
            log(Log.INFO, TAG, "Inserted media call record successfully. [Call Id]:" + callId);
        } else {
            log(Log.ERROR, TAG, "Insert call record failed. [Response code]:" + responseCode);
        }
    }
}
