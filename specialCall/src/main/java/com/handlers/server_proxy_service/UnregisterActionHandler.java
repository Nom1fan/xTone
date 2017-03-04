package com.handlers.server_proxy_service;

import android.util.Log;

import com.client.ConnectionToServer;
import com.event.EventReport;
import com.event.EventType;
import com.handlers.ActionHandler;
import com.model.request.UnRegisterRequest;
import com.utils.BroadcastUtils;

import java.io.IOException;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 20/12/2016.
 */
public class UnregisterActionHandler implements ActionHandler {

    private static final String TAG = UnregisterActionHandler.class.getSimpleName();
    private static final String URL_UNREGISTER = ROOT_URL + "/v1/UnRegister";

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        UnRegisterRequest unRegisterRequest = new UnRegisterRequest();
        unRegisterRequest.setLocale(Locale.getDefault().getLanguage());

        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();

        log(Log.INFO, TAG, "Initiating insert call record sequence...");
        int responseCode = connectionToServer.sendRequest(URL_UNREGISTER, unRegisterRequest);

        EventReport eventReport;
        if(responseCode == HttpStatus.SC_OK)
            eventReport = new EventReport(EventType.UNREGISTER_SUCCESS);
        else
            eventReport = new EventReport(EventType.UNREGISTER_FAILURE);

        BroadcastUtils.sendEventReportBroadcast(actionBundle.getCtx(), TAG, eventReport);
    }
}
