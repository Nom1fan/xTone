package com.handlers.logic_server_proxy_service;

import android.util.Log;

import com.client.ConnectionToServer;
import com.data.objects.ClearMediaData;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.model.request.NotifyMediaClearedRequest;
import com.data.objects.AppMeta;
import com.model.response.Response;

import java.io.IOException;
import java.lang.reflect.Type;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;
import static com.services.ServerProxyService.CLEAR_MEDIA_DATA;

/**
 * Created by Mor on 20/12/2016.
 */
public class NotifyMediaClearedActionHandler implements ActionHandler {

    private static final String URL_NOTIFY_MEDIA_CLEARED = ROOT_URL + "/v1/NotifyMediaCleared";
    private static final String TAG = NotifyMediaClearedActionHandler.class.getSimpleName();

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();

        ClearMediaData clearMediaData = (ClearMediaData) actionBundle.getIntent().getSerializableExtra(CLEAR_MEDIA_DATA);
        NotifyMediaClearedRequest request = new NotifyMediaClearedRequest(actionBundle.getRequest());
        request.setSourceLocale(clearMediaData.getSourceLocale());
        request.setSourceId(clearMediaData.getSourceId());
        request.setSpecialMediaType(clearMediaData.getSpecialMediaType());

        int responseCode = connectionToServer.sendRequest(URL_NOTIFY_MEDIA_CLEARED, request);
        if (responseCode != HttpStatus.SC_OK) {
            log(Log.ERROR, TAG, "Notify media cleared failed. [Response code]:" + responseCode);
        }
    }
}
