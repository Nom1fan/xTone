package com_international.handlers.server_proxy_service;

import android.util.Log;

import com_international.client.ConnectionToServerImpl;
import com_international.data.objects.ClearMediaData;
import com_international.handlers.ActionHandler;
import com_international.model.request.NotifyMediaClearedRequest;

import java.io.IOException;

import com_international.services.ServerProxyService;
import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 20/12/2016.
 */
public class NotifyMediaClearedActionHandler implements ActionHandler {

    private static final String URL_NOTIFY_MEDIA_CLEARED = ROOT_URL + "/v1/NotifyMediaCleared";
    private static final String TAG = NotifyMediaClearedActionHandler.class.getSimpleName();

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        ConnectionToServerImpl connectionToServer = actionBundle.getConnectionToServer();

        ClearMediaData clearMediaData = (ClearMediaData) actionBundle.getIntent().getSerializableExtra(ServerProxyService.CLEAR_MEDIA_DATA);
        NotifyMediaClearedRequest request = new NotifyMediaClearedRequest(actionBundle.getRequest());
        request.setLocale(clearMediaData.getSourceLocale());
        request.setSourceId(clearMediaData.getSourceId());
        request.setSpecialMediaType(clearMediaData.getSpecialMediaType());

        int responseCode = connectionToServer.sendRequest(URL_NOTIFY_MEDIA_CLEARED, request);
        if (responseCode != HttpStatus.SC_OK) {
            log(Log.ERROR, TAG, "Notify media cleared failed. [Response code]:" + responseCode);
        }
    }
}
