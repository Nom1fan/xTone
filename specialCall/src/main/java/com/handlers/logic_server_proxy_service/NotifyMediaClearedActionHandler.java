package com.handlers.logic_server_proxy_service;

import android.util.Log;

import com.client.ConnectionToServer;
import com.data.objects.Constants;
import com.data.objects.DataKeys;
import com.data.objects.SpecialMediaType;
import com.event.EventReport;
import com.event.EventType;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.model.request.ClearMediaRequest;
import com.model.request.NotifyMediaClearedRequest;
import com.model.response.AppMetaDTO;
import com.model.response.Response;
import com.utils.BroadcastUtils;
import com.utils.ContactsUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;
import static com.services.ServerProxyService.DESTINATION_ID;
import static com.services.ServerProxyService.SPECIAL_MEDIA_TYPE;
import static com.services.ServerProxyService.TRANSFER_DETAILS;

/**
 * Created by Mor on 20/12/2016.
 */
public class NotifyMediaClearedActionHandler implements ActionHandler {

    private static final String URL_NOTIFY_MEDIA_CLEARED = ROOT_URL + "/v1/NotifyMediaCleared";
    private static final String TAG = NotifyMediaClearedActionHandler.class.getSimpleName();
    private static final Type responseType = new TypeToken<Response<AppMetaDTO>>() {
    }.getType();

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();
        connectionToServer.setResponseType(responseType);

        HashMap tdData = (HashMap) actionBundle.getIntent().getSerializableExtra(TRANSFER_DETAILS);
        NotifyMediaClearedRequest request = new NotifyMediaClearedRequest(actionBundle.getRequest());
        request.setSourceLocale(tdData.get(DataKeys.SOURCE_LOCALE).toString());

        int responseCode = connectionToServer.send(URL_NOTIFY_MEDIA_CLEARED, request);
        if (responseCode != HttpStatus.SC_OK) {
            log(Log.ERROR, TAG, "Notify media cleared failed. [Response code]:" + responseCode);
        }
    }
}
