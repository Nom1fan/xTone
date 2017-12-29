package com.converters;

import com.data.objects.DefaultMediaData;
import com.data.objects.PendingDownloadData;
import com.enums.SpecialMediaType;
import com.files.media.MediaFile;
import com.utils.Utility;

/**
 * Created by Mor on 31/05/2017.
 */

public interface MediaDataConverter extends Utility {

    PendingDownloadData toPendingDownloadData(String sourceId, SpecialMediaType specialMediaType, DefaultMediaData defaultMediaData);

}
