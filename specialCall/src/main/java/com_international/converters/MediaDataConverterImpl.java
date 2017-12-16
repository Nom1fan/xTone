package com_international.converters;

import com_international.data.objects.DefaultMediaData;
import com_international.data.objects.PendingDownloadData;
import com_international.enums.SpecialMediaType;

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
