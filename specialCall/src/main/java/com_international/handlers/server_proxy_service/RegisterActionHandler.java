package com_international.handlers.server_proxy_service;

import android.util.Log;

import com_international.client.ConnectionToServerImpl;
import com_international.event.EventReport;
import com_international.event.EventType;
import com_international.handlers.ActionHandler;
import com_international.model.request.RegisterRequest;
import com_international.utils.BroadcastUtils;

import java.io.IOException;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;
import static com_international.services.ServerProxyService.SMS_CODE;

/**
 * Created by Mor on 20/12/2016.
 */
public class RegisterActionHandler implements ActionHandler {

    private static final String TAG = RegisterActionHandler.class.getSimpleName();
    private static final String URL_REGISTER = ROOT_URL + "/v1/Register";

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        int smsCode = actionBundle.getIntent().getIntExtra(SMS_CODE, 0);
        ConnectionToServerImpl connectionToServer = actionBundle.getConnectionToServer();
        RegisterRequest registerRequest = new RegisterRequest(actionBundle.getRequest());
        log(Log.INFO, TAG, "Initiating actionRegister sequence...");
        registerRequest.setSmsCode(smsCode);
        int responseCode = connectionToServer.sendRequest(URL_REGISTER, registerRequest);

        EventType eventType;
        String errMsg = null;
        if(responseCode == HttpStatus.SC_OK) {
            eventType = EventType.REGISTER_SUCCESS;
        }
        else {
            eventType = EventType.REGISTER_FAILURE;
            errMsg = "Registration failed. [Response code]:" + responseCode;
        }

        BroadcastUtils.sendEventReportBroadcast(actionBundle.getCtx(), TAG, new EventReport(eventType, errMsg, responseCode));
    }
}
