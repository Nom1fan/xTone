package com.actions;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.SharedConstants;
import DataObjects.SpecialMediaType;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;

/**
 * Created by Mor on 27/04/2016.
 */
public class ClientActionDownloadFile extends ClientAction {

    public ClientActionDownloadFile() {
        super(ClientActionType.DOWNLOAD_FILE);
    }

    @Override
    public EventReport doClientAction(Map data) throws IOException {

        String sourceId = data.get(DataKeys.SOURCE_ID).toString();
        String fileName = data.get(DataKeys.SOURCE_WITH_EXTENSION).toString();

        BufferedOutputStream bos = null;
        try
        {
            File folderPath = null;

            switch(SpecialMediaType.valueOf(data.get(DataKeys.SPECIAL_MEDIA_TYPE).toString()))
            {
                case CALLER_MEDIA:
                    folderPath = new File(SharedConstants.INCOMING_FOLDER + sourceId);
                    break;
                case PROFILE_MEDIA:
                    folderPath = new File(SharedConstants.OUTGOING_FOLDER + sourceId);
                    break;
                default:
                    throw new UnsupportedOperationException("Not yet implemented");

            }

            // Creating file and directories for downloaded file
            folderPath.mkdirs();
            String fileStoragePath =  folderPath.getAbsolutePath() + "/" + fileName;
            File newFile = new File(fileStoragePath);
            newFile.createNewFile();

            FileOutputStream fos = new FileOutputStream(fileStoragePath);
            bos = new BufferedOutputStream(fos);
            DataInputStream dis = new DataInputStream(_connectionToServer.getClientSocket().getInputStream());

            System.out.println("Reading data...");
            Object oFileSize = data.get(DataKeys.FILE_SIZE);
            byte[] buf = new byte[1024*8];
            long fileSize = oFileSize instanceof Long ? (Long)oFileSize : ((Double)oFileSize).longValue();
            int bytesRead;
            while (fileSize > 0 && (bytesRead = dis.read(buf, 0, (int)Math.min(buf.length, fileSize))) != -1)
            {
                bos.write(buf,0,bytesRead);
                fileSize -= bytesRead;
            }

            if(fileSize > 0)
                throw new IOException("download was stopped abruptly");

        }
        finally {
            if(bos!=null)
                bos.close();
        }

        String desc = "DOWNLOAD_SUCCESS. Filename:"+fileName;
        return new EventReport(EventType.DOWNLOAD_SUCCESS, desc, data);
    }
}
