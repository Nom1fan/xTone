package com.handlers.logic_server_proxy_service;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import com.client.ConnectionToServer;
import com.data.objects.Constants;
import com.data.objects.DataKeys;
import com.data.objects.PushEventKeys;
import com.data.objects.SpecialMediaType;
import com.event.EventReport;
import com.event.EventType;
import com.files.media.MediaFile;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.model.request.DownloadFileRequest;
import com.model.response.Response;
import com.utils.BroadcastUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpStatus;

import static android.content.Context.POWER_SERVICE;
import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 20/12/2016.
 */
public class DownloadFileActionHandler implements ActionHandler {

    private static final String TAG = DownloadFileActionHandler.class.getSimpleName();
    private static final Type responseType = new TypeToken<Response>() {
    }.getType();
    private static final String URL_DOWNLOAD = ROOT_URL + "/v1/DownloadFile";


    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();
        connectionToServer.setResponseType(responseType);
        DownloadFileRequest request = new DownloadFileRequest(actionBundle.getRequest());
        request.setDestinationId(request.getMessageInitiaterId());
        PowerManager powerManager = (PowerManager) actionBundle.getCtx().getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + "_wakeLock");
        wakeLock.acquire();
        HashMap pushData = (HashMap) actionBundle.getIntent().getSerializableExtra(PushEventKeys.PUSH_DATA);

        try {
            requestDownloadFromServer(actionBundle.getCtx(), connectionToServer, pushData, request);
        } finally {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private void requestDownloadFromServer(Context ctx, ConnectionToServer connectionToServer, HashMap pushData, DownloadFileRequest request) {
        Integer commId = Integer.valueOf(pushData.get(DataKeys.COMM_ID).toString());
        request.setCommId(commId);
        String fileName = pushData.get(DataKeys.SOURCE_WITH_EXTENSION).toString();
        SpecialMediaType specialMediaType = SpecialMediaType.valueOf(pushData.get(DataKeys.SPECIAL_MEDIA_TYPE).toString());
        request.setSourceId(pushData.get(DataKeys.SOURCE_ID).toString());
        String pathToDownload = resolvePathBySpecialMediaType(specialMediaType, request.getSourceId(), fileName);
        String sFileSize = pushData.get(DataKeys.FILE_SIZE).toString();
        long fileSize;
        try {
            fileSize = Long.valueOf(sFileSize);
        } catch (Exception e) {
            fileSize = Double.valueOf(sFileSize).longValue();
        }
        String destinationContactName = pushData.get(DataKeys.DESTINATION_CONTACT_NAME).toString();
        request.setDestinationContactName(destinationContactName);
        MediaFile.FileType fileType = MediaFile.FileType.valueOf(pushData.get(DataKeys.FILE_TYPE).toString());
        String sourceLocale = pushData.get(DataKeys.SOURCE_LOCALE).toString();
        request.setSourceLocale(sourceLocale);
        request.setFileType(fileType);
        request.setFilePathOnServer(pushData.get(DataKeys.FILE_PATH_ON_SERVER).toString());
        request.setFilePathOnSrcSd(pushData.get(DataKeys.FILE_PATH_ON_SRC_SD).toString());
        request.setSpecialMediaType(specialMediaType);

        boolean success = connectionToServer.download(URL_DOWNLOAD, pathToDownload, fileSize, request);
        if(success) {
            BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.DOWNLOAD_SUCCESS, pushData));
        }
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
