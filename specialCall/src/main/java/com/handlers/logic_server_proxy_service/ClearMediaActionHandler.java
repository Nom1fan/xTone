package com.handlers.logic_server_proxy_service;

import android.content.Context;
import android.util.Log;

import com.app.AppStateManager;
import com.client.ConnectionToServer;
import com.data.objects.Constants;
import com.enums.SpecialMediaType;
import com.event.EventReport;
import com.event.EventType;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.model.request.ClearMediaRequest;
import com.data.objects.AppMeta;
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
    private static final Type responseType = new TypeToken<Response<AppMeta>>() {
    }.getType();

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        Context ctx = actionBundle.getCtx();
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();
        connectionToServer.setResponseType(responseType);

        String destId = actionBundle.getIntent().getStringExtra(DESTINATION_ID);
        SpecialMediaType specialMediaType = (SpecialMediaType) actionBundle.getIntent().getSerializableExtra(SPECIAL_MEDIA_TYPE);

        ClearMediaRequest request = new ClearMediaRequest(actionBundle.getRequest());
        request.setLocale(Locale.getDefault().getLanguage());
        request.setDestinationId(destId);
        request.setSourceId(Constants.MY_ID(ctx));
        request.setSpecialMediaType(specialMediaType);
        request.setDestinationContactName(ContactsUtils.getContactName(ctx, destId));

        int responseCode = connectionToServer.sendRequest(URL_CLEAR_MEDIA, request);
        if(responseCode == HttpStatus.SC_OK) {
            AppStateManager.setAppState(ctx, TAG, AppStateManager.getAppPrevState(ctx));
            BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.CLEAR_SENT));
        }
        else {
            log(Log.ERROR, TAG, "Clear media failed. [Response code]:" + responseCode);
            BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.CLEAR_FAILURE, destId, specialMediaType));
        }

    }
}
