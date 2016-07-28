package com.server.actions.v1;

import com.server.actions.ServerAction;
import com.server.annotations.ServerActionAnno;
import com.server.handlers.SpMediaPathHandler;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import DataObjects.DataKeys;
import DataObjects.PushEventKeys;
import DataObjects.SpecialMediaType;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import com.server.lang.LangStrings;

/**
 * Created by Mor on 23/04/2016.
 */
@ServerActionAnno(actionType = ServerActionType.UPLOAD_FILE)
public class ServerActionUploadFile extends ServerAction {

    protected String destId;
    protected String destContact;

    protected Map<SpecialMediaType, SpMediaPathHandler> spMedia2PathHandlerMap = new HashMap<>();

    public ServerActionUploadFile() {
        super(ServerActionType.UPLOAD_FILE);
    }

    public ServerActionUploadFile(ServerActionType serverActionType) {
        super(serverActionType);
    }


    @Override
    public void doAction(Map data) throws IOException {
        
        StringBuilder fileFullPath = new StringBuilder();
        Path currentRelativePath = Paths.get("");

        // Working directory
        fileFullPath.append(currentRelativePath.toAbsolutePath().toString());

        FileManager managedFile = (FileManager) data.get(DataKeys.MANAGED_FILE);
        destId = (String) data.get(DataKeys.DESTINATION_ID);
        destContact = (String) data.get(DataKeys.DESTINATION_CONTACT_NAME);
        SpecialMediaType specialMediaType = (SpecialMediaType) data.get(DataKeys.SPECIAL_MEDIA_TYPE);

        HashMap<DataKeys, Object> dataForHandler = new HashMap<>();
        dataForHandler.put(DataKeys.FILE_FULL_PATH, fileFullPath);
        data.putAll(dataForHandler);

        initiateUploadfileFlow(data, fileFullPath, managedFile.getFileSize(), specialMediaType);

    }

    protected void initiateUploadfileFlow(Map data, StringBuilder fileFullPath, long fileSize, SpecialMediaType specialMediaType) {
        SpMediaPathHandler spMediaPathHandler = spMedia2PathHandlerMap.get(specialMediaType);
        spMediaPathHandler.appendPathForMedia(data);

        String infoMsg = "Initiating file upload. [Source]:" + messageInitiaterId +
                ". [Destination]:" + destId + "." +
                " [Special Media Type]:" + specialMediaType +
                " [File size]:" +
                FileManager.getFileSizeFormat(fileSize);
        logger.info(infoMsg);

        BufferedOutputStream bos = null;
        try {

            // Preparing file placeholder
            File newFile = new File(fileFullPath.toString());
            newFile.getParentFile().mkdirs();
            newFile.createNewFile();

            FileOutputStream fos = new FileOutputStream(newFile);
            bos = new BufferedOutputStream(fos);
            DataInputStream dis = new DataInputStream(clientConnection.getClientSocket().getInputStream());

            logger.info("Reading data...");
            byte[] buf = new byte[1024 * 8];
            int bytesRead;
            while (fileSize > 0 && (bytesRead = dis.read(buf, 0, (int) Math.min(buf.length, fileSize))) != -1) {
                bos.write(buf, 0, bytesRead);
                fileSize -= bytesRead;
            }

            if (fileSize > 0)
                throw new IOException("Upload was stopped abruptly");
            else if (fileSize < 0)
                throw new IOException("Read too many bytes. Upload seems corrupted.");


            // Informing source (uploader) that the file is on the way
            HashMap<DataKeys,Object> replyData = new HashMap<>();
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.UPLOAD_SUCCESS, data));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));

            // Inserting the record of the file upload, retrieving back the commId
            int commId = dao.insertMediaTransferRecord(data);
            logger.info("commId returned:" + commId);

            // Sending file to destination
            data.put(DataKeys.COMM_ID, commId);
            data.put(DataKeys.FILE_PATH_ON_SERVER, fileFullPath.toString());
            String destToken = dao.getUserRecord(destId).getToken();
            String pushEventAction = PushEventKeys.PENDING_DOWNLOAD;
            boolean sent = pushSender.sendPush(destToken, pushEventAction, data);

            if (!sent) {
                sendMediaUndeliveredMsgToUploader(data);
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            sendMediaUndeliveredMsgToUploader(data);

        } finally {
            if (bos != null)
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    @Autowired
    public void initMap(List<SpMediaPathHandler> spMediaPathHandlerList) {
        for (SpMediaPathHandler spMediaPathHandler : spMediaPathHandlerList) {
            spMedia2PathHandlerMap.put(spMediaPathHandler.getHandledSpMediaType(), spMediaPathHandler);
        }
    }

    protected void sendMediaUndeliveredMsgToUploader(Map data) {

        logger.severe("Upload from [Source]:" + messageInitiaterId + " to [Destination]:" + destId + " Failed.");

        LangStrings strings = stringsFactory.getStrings((String) data.get(DataKeys.SOURCE_LOCALE));
        String title = strings.media_undelivered_title();

        String dest = (!destContact.equals("") ? destContact : destId);
        String errMsg = String.format(strings.media_undelivered_body(), dest);

        String destHtml = "<b><font color=\"#00FFFF\">" + (!destContact.equals("") ? destContact : destId) + "</font></b>";
        String errMsgHtml = String.format(strings.media_undelivered_body(), destHtml);

        // Packing HTML string as push data
        HashMap<DataKeys, Object> replyData = new HashMap();
        replyData.put(DataKeys.HTML_STRING, errMsgHtml);

        String initiaterToken = null;
        try {
            initiaterToken = dao.getUserRecord(messageInitiaterId).getToken();
        } catch (SQLException e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Could not notify uploader that media was undelivered", e);
        }

        // Informing source (uploader) that the file was not sent to destination
        pushSender.sendPush(initiaterToken, PushEventKeys.SHOW_ERROR, title, errMsg, replyData);
    }
}
