package com.handlers.server_proxy_service;

import android.util.Log;

import com.client.ConnectionToServer;
import com.data.objects.PendingDownloadData;
import com.handlers.ActionHandler;
import com.model.request.NotifyMediaReadyRequest;

import java.io.IOException;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;
import static com.services.ServerProxyService.PENDING_DOWNLOAD_DATA;

/**
 * Created by Mor on 20/12/2016.
 */
public class NotifyMediaReadyActionHandler implements ActionHandler {

    private static final String URL_NOTIFY_MEDIA_READY = ROOT_URL + "/v1/NotifyMediaReady";
    private static final String TAG = NotifyMediaReadyActionHandler.class.getSimpleName();


    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();

        PendingDownloadData pendingDownloadData = (PendingDownloadData) actionBundle.getIntent().getSerializableExtra(PENDING_DOWNLOAD_DATA);
        NotifyMediaReadyRequest request = new NotifyMediaReadyRequest(actionBundle.getRequest());
        request.setLocale(pendingDownloadData.getSourceLocale());
        request.setSourceId(pendingDownloadData.getSourceId());
        request.setSpecialMediaType(pendingDownloadData.getSpecialMediaType());
        request.setDestinationContactName(pendingDownloadData.getDestinationContactName());
        request.setFilePathOnSrcSd(pendingDownloadData.getFilePathOnSrcSd());
        request.setDestinationId(pendingDownloadData.getDestinationId());
        request.setCommId(pendingDownloadData.getCommId());

        int responseCode = connectionToServer.sendRequest(URL_NOTIFY_MEDIA_READY, request);
        if (responseCode != HttpStatus.SC_OK) {
            log(Log.ERROR, TAG, "Notify media ready failed. [Response code]:" + responseCode);
        }
    }
}
