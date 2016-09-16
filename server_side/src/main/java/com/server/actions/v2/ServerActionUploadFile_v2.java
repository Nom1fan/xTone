package com.server.actions.v2;

import com.server.annotations.ServerActionAnno;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.SpecialMediaType;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 23/04/2016.
 */
@ServerActionAnno(actionType = ServerActionType.UPLOAD_FILE_V2)
public class ServerActionUploadFile_v2 extends com.server.actions.v1.ServerActionUploadFile {

    public ServerActionUploadFile_v2() {
        super(ServerActionType.UPLOAD_FILE_V2);
    }

    @Override
    public void doAction(Map data) throws IOException {

        StringBuilder fileFullPath = new StringBuilder();
        Path currentRelativePath = Paths.get("");

        // Working directory
        fileFullPath.append(currentRelativePath.toAbsolutePath().toString());

        destId = (String) data.get(DataKeys.DESTINATION_ID);
        destContact = (String) data.get(DataKeys.DESTINATION_CONTACT_NAME);
        SpecialMediaType specialMediaType = (SpecialMediaType) data.get(DataKeys.SPECIAL_MEDIA_TYPE);
        long fileSize = (long) data.get(DataKeys.FILE_SIZE);

        HashMap<DataKeys, Object> dataForHandler = new HashMap<>();
        dataForHandler.put(DataKeys.FILE_FULL_PATH, fileFullPath);
        data.putAll(dataForHandler);

        initiateUploadfileFlow(data, fileFullPath, fileSize, specialMediaType);

    }


}
