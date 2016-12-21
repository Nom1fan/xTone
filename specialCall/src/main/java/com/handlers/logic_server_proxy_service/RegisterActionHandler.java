package com.handlers.logic_server_proxy_service;

import android.os.Build;
import android.util.Log;

import com.client.ConnectionToServer;
import com.event.EventReport;
import com.event.EventType;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.model.request.RegisterRequest;
import com.model.response.Response;
import com.services.LogicServerProxyService;
import com.utils.BroadcastUtils;
import com.utils.SpecialDevicesUtils;

import java.io.IOException;
import java.lang.reflect.Type;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 20/12/2016.
 */
public class RegisterActionHandler implements ActionHandler {

    private static final String TAG = RegisterActionHandler.class.getSimpleName();
    private static final Type responseType = new TypeToken<Response>() {
    }.getType();
    private static final String URL_REGISTER = ROOT_URL + "/v1/Register";

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        int smsCode = actionBundle.getIntent().getIntExtra(LogicServerProxyService.SMS_CODE, 0);
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();
        connectionToServer.setResponseType(responseType);
        RegisterRequest registerRequest = new RegisterRequest(actionBundle.getRequest());

        log(Log.INFO, TAG, "Initiating actionRegister sequence...");
        registerRequest.setDeviceModel(SpecialDevicesUtils.getDeviceName());
        registerRequest.setAndroidVersion(Build.VERSION.RELEASE);
        registerRequest.setSmsCode(smsCode);
        int responseCode = connectionToServer.send(URL_REGISTER, registerRequest);

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
