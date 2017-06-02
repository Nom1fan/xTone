package com.handlers.server_proxy_service;

import android.os.PowerManager;
import android.support.annotation.NonNull;

import com.client.ConnectionToServer;
import com.data.objects.DefaultMediaData;
import com.data.objects.DownloadData;
import com.data.objects.PendingDownloadData;
import com.data.objects.PushEventKeys;
import com.event.EventReport;
import com.event.EventType;
import com.handlers.ActionHandler;
import com.model.request.DownloadFileRequest;
import com.utils.BroadcastUtils;
import com.utils.MediaFileUtils;
import com.utils.UtilityFactory;

import java.io.IOException;

import static android.content.Context.POWER_SERVICE;
import static com.crashlytics.android.Crashlytics.log;
import static com.services.ServerProxyService.*;

/**
 * Created by Mor on 20/12/2016.
 */
public class DownloadFileActionHandler implements ActionHandler {

    private static final String TAG = DownloadFileActionHandler.class.getSimpleName();

    private MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);

    private static final String URL_DOWNLOAD = ROOT_URL + "/v1/DownloadFile";


    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();

        PowerManager powerManager = (PowerManager) actionBundle.getCtx().getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + "_wakeLock");
        wakeLock.acquire();
        PendingDownloadData pendingDownloadData = (PendingDownloadData) actionBundle.getIntent().getSerializableExtra(PushEventKeys.PUSH_DATA);
        DefaultMediaData defaultMediaData = (DefaultMediaData) actionBundle.getIntent().getSerializableExtra(DEFAULT_MEDIA_DATA);
        DownloadData downloadData = prepareDownloadData(pendingDownloadData, defaultMediaData);

        try {
            boolean success = requestDownloadFromServer(connectionToServer, downloadData, actionBundle);
            if(success) {
                BroadcastUtils.sendEventReportBroadcast(actionBundle.getCtx(), TAG, new EventReport(EventType.DOWNLOAD_SUCCESS, downloadData));
            }
            else {
                //TODO send NotifyDownloadFailed
            }
        } finally {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private boolean requestDownloadFromServer(ConnectionToServer connectionToServer, DownloadData downloadData, ActionBundle actionBundle) {
        PendingDownloadData pendingDownloadData = downloadData.getPendingDownloadData();
        DefaultMediaData defaultMediaData = downloadData.getDefaultMediaData();

        DownloadFileRequest request = prepareRequest(pendingDownloadData, actionBundle);

        long fileSize = pendingDownloadData.getMediaFile().getSize();
        String pathToDownload = mediaFileUtils.resolvePathBySpecialMediaType(pendingDownloadData, defaultMediaData);
        return connectionToServer.download(URL_DOWNLOAD, pathToDownload, fileSize, request);
    }

    private DownloadFileRequest prepareRequest(PendingDownloadData pendingDownloadData, ActionBundle actionBundle) {
        DownloadFileRequest request = new DownloadFileRequest(actionBundle.getRequest());
        request.setDestinationId(request.getUser().getUid());
        request.setDestinationContactName(pendingDownloadData.getDestinationContactName());
        request.setSourceId(pendingDownloadData.getSourceId());
        request.setCommId(pendingDownloadData.getCommId());
        request.setLocale(pendingDownloadData.getSourceLocale());
        request.setFilePathOnServer(pendingDownloadData.getFilePathOnServer());
        request.setFilePathOnSrcSd(pendingDownloadData.getFilePathOnSrcSd());
        request.setSpecialMediaType(pendingDownloadData.getSpecialMediaType());
        return request;
    }

    @NonNull
    private DownloadData prepareDownloadData(PendingDownloadData pendingDownloadData, DefaultMediaData defaultMediaData) {
        DownloadData downloadData = new DownloadData();
        downloadData.setPendingDownloadData(pendingDownloadData);
        downloadData.setDefaultMediaData(defaultMediaData);
        return downloadData;
    }

}
