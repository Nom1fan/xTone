package com.handlers.logic_server_proxy_service;

import android.os.PowerManager;

import com.client.ConnectionToServer;
import com.data.objects.Constants;
import com.data.objects.PendingDownloadData;
import com.data.objects.PushEventKeys;
import com.enums.SpecialMediaType;
import com.event.EventReport;
import com.event.EventType;
import com.handlers.ActionHandler;
import com.model.request.DownloadFileRequest;
import com.utils.BroadcastUtils;

import java.io.IOException;

import static android.content.Context.POWER_SERVICE;
import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 20/12/2016.
 */
public class DownloadFileActionHandler implements ActionHandler {

    private static final String TAG = DownloadFileActionHandler.class.getSimpleName();
    private static final String URL_DOWNLOAD = ROOT_URL + "/v1/DownloadFile";


    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();
        DownloadFileRequest request = new DownloadFileRequest(actionBundle.getRequest());
        request.setDestinationId(request.getUser().getUid());
        PowerManager powerManager = (PowerManager) actionBundle.getCtx().getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + "_wakeLock");
        wakeLock.acquire();
        PendingDownloadData pendingDownloadData = (PendingDownloadData) actionBundle.getIntent().getSerializableExtra(PushEventKeys.PUSH_DATA);

        try {
            boolean success = requestDownloadFromServer(connectionToServer, pendingDownloadData, request);
            if(success) {
                BroadcastUtils.sendEventReportBroadcast(actionBundle.getCtx(), TAG, new EventReport(EventType.DOWNLOAD_SUCCESS, pendingDownloadData));
            }
        } finally {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private boolean requestDownloadFromServer(ConnectionToServer connectionToServer, PendingDownloadData pendingDownloadData, DownloadFileRequest request) {
        request.setDestinationContactName(pendingDownloadData.getDestinationContactName());
        request.setSourceId(pendingDownloadData.getSourceId());
        request.setCommId(pendingDownloadData.getCommId());
        request.setLocale(pendingDownloadData.getSourceLocale());
        request.setFilePathOnServer(pendingDownloadData.getFilePathOnServer());
        request.setFilePathOnSrcSd(pendingDownloadData.getFilePathOnSrcSd());
        request.setSpecialMediaType(pendingDownloadData.getSpecialMediaType());

        String fileName = pendingDownloadData.getSourceId() + "." + pendingDownloadData.getMediaFile().getExtension();
        long fileSize = pendingDownloadData.getMediaFile().getFileSize();
        String pathToDownload = resolvePathBySpecialMediaType(pendingDownloadData.getSpecialMediaType(), pendingDownloadData.getSourceId(), fileName);
        return connectionToServer.download(URL_DOWNLOAD, pathToDownload, fileSize, request);
    }

    private String resolvePathBySpecialMediaType(SpecialMediaType specialMediaType, String folderName, String fileName) {
        String filePath;
        switch (specialMediaType) {
            case CALLER_MEDIA:
                filePath = Constants.INCOMING_FOLDER + folderName + "/" + fileName;
                break;
            case PROFILE_MEDIA:
                filePath = Constants.OUTGOING_FOLDER + folderName + "/" + fileName;
                break;
            default:
                throw new UnsupportedOperationException("Not yet implemented");

        }
        return filePath;
    }
}
