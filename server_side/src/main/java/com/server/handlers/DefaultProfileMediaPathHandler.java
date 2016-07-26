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
public class DefaultProfileMediaPathHandler implements SpMediaPathHandler {

    @Override
    public StringBuilder appendPathForMedia(Map<DataKeys, Object> data) {
        StringBuilder root = (StringBuilder) data.get(DataKeys.FILE_FULL_PATH);
        String messageInitiaterId = (String) data.get(DataKeys.DESTINATION_ID);
        String extension = (String) data.get(DataKeys.EXTENSION);

        root.append(SharedConstants.UPLOAD_FOLDER).append(messageInitiaterId).append("\\").
                append(SharedConstants.MY_DEFAULT_PROFILE_MEDIA_FOLDER).
                append(SharedConstants.MY_DEFAULT_PROFILE_MEDIA_FILENAME).
                append(extension);
        return root;
    }

    @Override
    public SpecialMediaType getHandledSpMediaType() {
        return SpecialMediaType.MY_DEFAULT_PROFILE_MEDIA;
    }
}
