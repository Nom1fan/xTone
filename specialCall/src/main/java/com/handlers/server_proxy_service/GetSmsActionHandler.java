package com.handlers.server_proxy_service;

import android.util.Log;

import com.client.ConnectionToServerImpl;
import com.event.EventReport;
import com.event.EventType;
import com.handlers.ActionHandler;
import com.model.request.GetSmsRequest;
import com.utils.BroadcastUtils;

import java.io.IOException;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;
import static com.services.ServerProxyService.INTERNATIONAL_PHONE;

/**
 * Created by Mor on 20/12/2016.
 */
public class GetSmsActionHandler implements ActionHandler {

    private static final String TAG = GetSmsActionHandler.class.getSimpleName();
    private static final String URL_GET_SMS_AUTH = ROOT_URL + "/v1/GetSmsAuthCode";

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        String interPhoneNumber = actionBundle.getIntent().getStringExtra(INTERNATIONAL_PHONE);
        ConnectionToServerImpl connectionToServer = actionBundle.getConnectionToServer();
        GetSmsRequest getSmsRequest = new GetSmsRequest(actionBundle.getRequest());
        getSmsRequest.setLocale(Locale.getDefault().getLanguage());
        getSmsRequest.setInternationalPhoneNumber(interPhoneNumber);
        int responseCode = connectionToServer.sendRequest(URL_GET_SMS_AUTH, getSmsRequest);

        if(responseCode== HttpStatus.SC_OK) {
            BroadcastUtils.sendEventReportBroadcast(actionBundle.getCtx(), TAG, new EventReport(EventType.GET_SMS_CODE_SUCCESS));
        }
        else {
            log(Log.ERROR, TAG, "Get SMS failed. [Response code]:" + responseCode);
            BroadcastUtils.sendEventReportBroadcast(actionBundle.getCtx(), TAG, new EventReport(EventType.GET_SMS_CODE_FAILURE));
        }
    }
}
