package com_international.converters;

import com_international.data.objects.DefaultMediaData;
import com_international.data.objects.PendingDownloadData;
import com_international.enums.SpecialMediaType;
import com_international.utils.Utility;

/**
 * Created by Mor on 31/05/2017.
 */

public interface MediaDataConverter extends Utility {

    PendingDownloadData toPendingDownloadData(String sourceId, SpecialMediaType specialMediaType, DefaultMediaData defaultMediaData);

}
