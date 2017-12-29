package com.handlers.server_proxy_service;

import android.util.Log;

import com.client.ConnectionToServerImpl;
import com.event.EventReport;
import com.event.EventType;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.data.objects.AppMeta;
import com.model.response.Response;
import com.utils.BroadcastUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 20/12/2016.
 */
public class GetAppRecordActionHandler implements ActionHandler {

    private static final String URL_GET_APP_RECORD = ROOT_URL + "/v1/GetAppMeta";
    private static final String TAG = GetAppRecordActionHandler.class.getSimpleName();
    private static final Type responseType = new TypeToken<Response<AppMeta>>() {
    }.getType();

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        ConnectionToServerImpl connectionToServer = actionBundle.getConnectionToServer();
        connectionToServer.setResponseType(responseType);
        actionBundle.getRequest().setLocale(Locale.getDefault().getLanguage());
        int responseCode = connectionToServer.sendRequest(URL_GET_APP_RECORD, actionBundle.getRequest());

        if (responseCode == HttpStatus.SC_OK) {
            Response<AppMeta> response = connectionToServer.readResponse();
            EventReport eventReport = new EventReport(EventType.APP_RECORD_RECEIVED, response.getResult().getLastSupportedAppVersion());
            BroadcastUtils.sendEventReportBroadcast(actionBundle.getCtx(), TAG, eventReport);
        } else {
            log(Log.ERROR, TAG, "Get App Record failed. [Response code]:" + responseCode);
        }
    }
}
