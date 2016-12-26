package com.handlers.logic_server_proxy_service;

import android.util.Log;

import com.client.ConnectionToServer;
import com.event.EventReport;
import com.event.EventType;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.model.request.GetSmsRequest;
import com.model.response.Response;
import com.utils.BroadcastUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;
import static com.services.ServerProxyService.INTERNATIONAL_PHONE;

/**
 * Created by Mor on 20/12/2016.
 */
public class GetSmsActionHandler implements ActionHandler {

    private static final String TAG = GetSmsActionHandler.class.getSimpleName();
    private static final Type responseType = new TypeToken<Response>() {}.getType();
    private static final String URL_GET_SMS_AUTH = ROOT_URL + "/v1/GetSmsAuthCode";

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        String interPhoneNumber = actionBundle.getIntent().getStringExtra(INTERNATIONAL_PHONE);
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();
        connectionToServer.setResponseType(responseType);
        GetSmsRequest getSmsRequest = new GetSmsRequest(actionBundle.getRequest());
        getSmsRequest.setSourceLocale(Locale.getDefault().getLanguage());
        getSmsRequest.setInternationalPhoneNumber(interPhoneNumber);
        int responseCode = connectionToServer.send(URL_GET_SMS_AUTH, getSmsRequest);

        if(responseCode== HttpStatus.SC_OK) {
            BroadcastUtils.sendEventReportBroadcast(actionBundle.getCtx(), TAG, new EventReport(EventType.GET_SMS_CODE_SUCCESS));
        }
        else {
            log(Log.ERROR, TAG, "Get SMS failed. [Response code]:" + responseCode);
            BroadcastUtils.sendEventReportBroadcast(actionBundle.getCtx(), TAG, new EventReport(EventType.GET_SMS_CODE_FAILURE));
        }
    }
}
