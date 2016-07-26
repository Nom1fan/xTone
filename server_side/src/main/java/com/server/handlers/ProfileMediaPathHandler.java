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
public class ProfileMediaPathHandler implements SpMediaPathHandler {

    @Override
    public StringBuilder appendPathForMedia(Map<DataKeys, Object> data) {
        String destId = (String) data.get(DataKeys.DESTINATION_ID);
        StringBuilder root = (StringBuilder) data.get(DataKeys.FILE_FULL_PATH);
        String extension = (String) data.get(DataKeys.EXTENSION);
        String srcWithExtension = destId + "." + extension;

        root.append(SharedConstants.UPLOAD_FOLDER).append(destId).append("\\").
                append(SharedConstants.PROFILE_MEDIA_RECEIVED_FOLDER).append(srcWithExtension);
        return root;
    }

    @Override
    public SpecialMediaType getHandledSpMediaType() {
        return SpecialMediaType.PROFILE_MEDIA;
    }
}
