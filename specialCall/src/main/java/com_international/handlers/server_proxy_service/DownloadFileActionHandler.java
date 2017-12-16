package com_international.handlers.server_proxy_service;

import android.content.Context;
import android.os.PowerManager;
import android.support.annotation.NonNull;

import com_international.client.ConnectionToServerImpl;
import com_international.data.objects.DefaultMediaData;
import com_international.data.objects.DownloadData;
import com_international.data.objects.PendingDownloadData;
import com_international.data.objects.PushEventKeys;
import com_international.event.EventReport;
import com_international.event.EventType;
import com_international.handlers.ActionHandler;
import com_international.model.request.DownloadFileRequest;
import com_international.utils.BroadcastUtils;
import com_international.utils.MediaFileUtils;
import com_international.utils.PowerManagerUtils;
import com_international.utils.UtilityFactory;

import java.io.File;
import java.io.IOException;

import static com.crashlytics.android.Crashlytics.log;
import static com_international.services.ServerProxyService.*;

/**
 * Created by Mor on 20/12/2016.
 */
@Deprecated
/**
 * Deprecated - This handler should only know how to download a file given a path to save the file in and a URL to download from
 */
public class DownloadFileActionHandler implements ActionHandler {

    private static final String TAG = DownloadFileActionHandler.class.getSimpleName();

    private MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);

    private PowerManagerUtils powerManagerUtils = UtilityFactory.instance().getUtility(PowerManagerUtils.class);

    private static final String URL_DOWNLOAD = ROOT_URL + "/v1/DownloadFile";

    public static final String PHONE = "PHONE";


    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        ConnectionToServerImpl connectionToServer = actionBundle.getConnectionToServer();
        Context context = actionBundle.getCtx();

        PendingDownloadData pendingDownloadData = (PendingDownloadData) actionBundle.getIntent().getSerializableExtra(PushEventKeys.PUSH_DATA);
        DefaultMediaData defaultMediaData = (DefaultMediaData) actionBundle.getIntent().getSerializableExtra(DEFAULT_MEDIA_DATA);

        String pathToDownload;
        if (defaultMediaData != null) {
            String phoneNumber = pendingDownloadData.getSourceId();
            pathToDownload = mediaFileUtils.resolvePathBySpecialMediaType(phoneNumber, pendingDownloadData.getSpecialMediaType(), defaultMediaData);
        }
        else {
            pathToDownload = mediaFileUtils.resolvePathBySpecialMediaType(pendingDownloadData);
        }
        DownloadData downloadData = prepareDownloadData(pendingDownloadData, defaultMediaData, pathToDownload);

        PowerManager.WakeLock wakeLock = powerManagerUtils.getWakeLock(context);
        try {
            wakeLock.acquire();
            boolean success = requestDownloadFromServer(connectionToServer, downloadData, actionBundle);
            if (success) {
                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.DOWNLOAD_SUCCESS, downloadData));
            } else {
                //TODO send NotifyDownloadFailed
            }
        } finally {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private boolean requestDownloadFromServer(ConnectionToServerImpl connectionToServer, DownloadData downloadData, ActionBundle actionBundle) {
        PendingDownloadData pendingDownloadData = downloadData.getPendingDownloadData();
        String pathToDownload = pendingDownloadData.getMediaFile().getFile().getPath();

        DownloadFileRequest request = prepareRequest(pendingDownloadData, actionBundle);

        long fileSize = pendingDownloadData.getMediaFile().getSize();
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
    private DownloadData prepareDownloadData(PendingDownloadData pendingDownloadData, DefaultMediaData defaultMediaData, String pathToDownload) {
        DownloadData downloadData = new DownloadData();
        pendingDownloadData.getMediaFile().setFile(new File(pathToDownload));
        downloadData.setPendingDownloadData(pendingDownloadData);
        downloadData.setDefaultMediaData(defaultMediaData);
        return downloadData;
    }

}
