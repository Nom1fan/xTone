package com.converters;

import android.support.annotation.NonNull;

import com.data.objects.Constants;
import com.data.objects.DefaultMediaData;
import com.data.objects.PendingDownloadData;
import com.files.media.MediaFile;

/**
 * Created by Mor on 31/05/2017.
 */

public class MediaDataConverterImpl implements MediaDataConverter {

    @Override
    public PendingDownloadData toPendingDownloadData(DefaultMediaData defaultMediaData) {
        PendingDownloadData pendingDownloadData = new PendingDownloadData();
        pendingDownloadData.setSpecialMediaType(defaultMediaData.getSpecialMediaType());
        pendingDownloadData.setFilePathOnServer(defaultMediaData.getFilePathOnServer());
        pendingDownloadData.setMediaFile(defaultMediaData.getMediaFile());
        pendingDownloadData.setSourceId(defaultMediaData.getUid());
        pendingDownloadData.setCommId(defaultMediaData.getCommId());
        pendingDownloadData.setFilePathOnSrcSd("N/A");
        return pendingDownloadData;
    }

    @Override
    public MediaFile toMediaFile(DefaultMediaData defaultMediaData) {
        return defaultMediaData.getMediaFile();
    }
}
