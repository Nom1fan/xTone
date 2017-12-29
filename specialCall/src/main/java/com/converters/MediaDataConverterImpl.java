package com.converters;

import android.support.annotation.NonNull;

import com.data.objects.Constants;
import com.data.objects.DefaultMediaData;
import com.data.objects.PendingDownloadData;
import com.enums.SpecialMediaType;
import com.files.media.MediaFile;

/**
 * Created by Mor on 31/05/2017.
 */

public class MediaDataConverterImpl implements MediaDataConverter {

    @Override
    public PendingDownloadData toPendingDownloadData(String sourceId, SpecialMediaType specialMediaType, DefaultMediaData defaultMediaData) {
        PendingDownloadData pendingDownloadData = new PendingDownloadData();
        pendingDownloadData.setSpecialMediaType(specialMediaType);
        pendingDownloadData.setFilePathOnServer(defaultMediaData.getFilePathOnServer());
        pendingDownloadData.setMediaFile(defaultMediaData.getMediaFile());
        pendingDownloadData.setSourceId(sourceId);
        pendingDownloadData.setFilePathOnSrcSd("N/A");
        return pendingDownloadData;
    }
}
