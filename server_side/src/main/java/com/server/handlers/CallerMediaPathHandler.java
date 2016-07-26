package com.server.handlers;

import org.springframework.stereotype.Component;

import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.SharedConstants;
import DataObjects.SpecialMediaType;

/**
 * Created by Mor on 25/07/2016.
 */
@Component
public class CallerMediaPathHandler implements SpMediaPathHandler {

    @Override
    public StringBuilder appendPathForMedia(Map<DataKeys, Object> data) {
        String destId = (String) data.get(DataKeys.DESTINATION_ID);
        StringBuilder root = (StringBuilder) data.get(DataKeys.FILE_FULL_PATH);
        String srcWithExtension = (String) data.get(DataKeys.SOURCE_WITH_EXTENSION);

        // Caller Media is saved in the destination's caller media folder,
        root.append(SharedConstants.UPLOAD_FOLDER).append(destId).append("\\").
                append(SharedConstants.CALLER_MEDIA_FOLDER).append(srcWithExtension);
        return root;
    }

    @Override
    public SpecialMediaType getHandledSpMediaType() {
        return SpecialMediaType.CALLER_MEDIA;
    }
}
