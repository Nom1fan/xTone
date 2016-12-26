package com.handlers.logic_server_proxy_service;

import android.util.Log;

import com.client.ConnectionToServer;
import com.data.objects.Constants;
import com.data.objects.SpecialMediaType;
import com.event.EventReport;
import com.event.EventType;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.model.request.ClearMediaRequest;
import com.model.response.AppMetaDTO;
import com.model.response.Response;
import com.utils.BroadcastUtils;
import com.utils.ContactsUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;
import static com.services.ServerProxyService.DESTINATION_ID;
import static com.services.ServerProxyService.SPECIAL_MEDIA_TYPE;

/**
 * Created by Mor on 20/12/2016.
 */
public class ClearMediaActionHandler implements ActionHandler {

    private static final String URL_CLEAR_MEDIA = ROOT_URL + "/v1/ClearMedia";
    private static final String TAG = ClearMediaActionHandler.class.getSimpleName();
    private static final Type responseType = new TypeToken<Response<AppMetaDTO>>() {
    }.getType();

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();
        connectionToServer.setResponseType(responseType);

        String destId = actionBundle.getIntent().getStringExtra(DESTINATION_ID);
        SpecialMediaType specialMediaType = (SpecialMediaType) actionBundle.getIntent().getSerializableExtra(SPECIAL_MEDIA_TYPE);

        ClearMediaRequest request = new ClearMediaRequest(actionBundle.getRequest());
        request.setSourceLocale(Locale.getDefault().getLanguage());
        request.setDestinationId(destId);
        request.setSourceId(Constants.MY_ID(actionBundle.getCtx()));
        request.setSpecialMediaType(specialMediaType);
        request.setDestinationContactName(ContactsUtils.getContactName(actionBundle.getCtx(), destId));

        int responseCode = connectionToServer.send(URL_CLEAR_MEDIA, request);

        EventType eventType;
        if (responseCode == HttpStatus.SC_OK) {
            eventType = EventType.CLEAR_SUCCESS;
        } else {
            log(Log.ERROR, TAG, "Get App Record failed. [Response code]:" + responseCode);
            eventType = EventType.CLEAR_FAILURE;
        }
        BroadcastUtils.sendEventReportBroadcast(actionBundle.getCtx(), TAG, new EventReport(eventType, destId, specialMediaType));
    }
}
