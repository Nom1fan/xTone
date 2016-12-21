package com.handlers.logic_server_proxy_service;

import android.util.Log;

import com.client.ConnectionToServer;
import com.data.objects.CallRecord;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.model.request.InsertMediaCallRecordRequest;
import com.model.response.Response;
import com.services.LogicServerProxyService;

import java.io.IOException;
import java.lang.reflect.Type;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 20/12/2016.
 */
public class InsertCallRecordActionHandler implements ActionHandler {

    private static final String TAG = InsertCallRecordActionHandler.class.getSimpleName();
    private static final Type responseType = new TypeToken<Response>() {
    }.getType();
    private static final String URL_INSERT_CALL_RECORD = ROOT_URL + "/v1/InsertMediaCallRecord";

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        CallRecord callRecord = (CallRecord) actionBundle.getIntent().getSerializableExtra(LogicServerProxyService.CALL_RECORD);
        InsertMediaCallRecordRequest request = new InsertMediaCallRecordRequest(actionBundle.getRequest());
        request.setCallRecord(callRecord);

        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();
        connectionToServer.setResponseType(responseType);


        log(Log.INFO, TAG, "Initiating insert call record sequence...");
        int responseCode = connectionToServer.send(URL_INSERT_CALL_RECORD, request);

        if(responseCode != HttpStatus.SC_OK) {
            log(Log.ERROR, TAG, "Insert call record failed. [Response code]:" + responseCode);
        }
    }
}
