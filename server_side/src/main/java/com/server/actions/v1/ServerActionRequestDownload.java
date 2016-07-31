package com.server.actions.v1;

import com.server.actions.ServerAction;
import com.server.annotations.ServerActionAnno;
import com.server.database.DAO;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import DataObjects.DataKeys;
import DataObjects.PushEventKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import Exceptions.DownloadRequestFailedException;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import com.server.lang.LangStrings;

/**
 * Created by Mor on 23/04/2016.
 */
@ServerActionAnno(actionType = ServerActionType.REQUEST_DOWNLOAD)
public class ServerActionRequestDownload extends ServerAction {

    private LangStrings strings;
    private String sourceId;
    private String destId;
    private String destContact;
    private int commId;

    public ServerActionRequestDownload() {
        super(ServerActionType.REQUEST_DOWNLOAD);
    }

    @PostConstruct
    public void init() {
        strings = stringsFactory.getStrings(LangStrings.Languages.ENGLISH.toString());
    }

    @Override
    public void doAction(Map data) throws IOException {

        Object oCommId = data.get(DataKeys.COMM_ID);
        commId = oCommId instanceof Double ? ((Double) oCommId).intValue() : (int) oCommId;
        sourceId = (String) data.get(DataKeys.SOURCE_ID);
        destId = (String) data.get(DataKeys.DESTINATION_ID);
        destContact = (String) data.get(DataKeys.DESTINATION_CONTACT_NAME);
        String sourceLocale = (String) data.get(DataKeys.SOURCE_LOCALE);
        String filePathOnServer = (String) data.get(DataKeys.FILE_PATH_ON_SERVER);

        if (sourceLocale != null)
            strings = stringsFactory.getStrings(sourceLocale);

        logger.info(messageInitiaterId + " is requesting download from:" + sourceId + ". File path on server:" + filePathOnServer + "...");

        initiateDownloadFlow(data, filePathOnServer);
    }

    private void initiateDownloadFlow(Map data, String filePathOnServer) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = initiateDownload((HashMap) data, filePathOnServer, bis);

            informSrcOfSuccess(data);

            // Marking in communication history record that the transfer was successful
            char TRUE = '1';
            dao.updateMediaTransferRecord(commId, DAO.COL_TRANSFER_SUCCESS, TRUE);


        } catch (DownloadRequestFailedException | SQLException e) {

            handleDownloadFailure(e);

        } catch (IOException e) {
            handleDownloadFailure(e);
            throw e;  // -------------> In case of IOException we need to notify the server infra
        } finally {
            if (bis != null)
                bis.close();
        }
    }

    private BufferedInputStream initiateDownload(HashMap data, String _filePathOnServer, BufferedInputStream bis) throws DownloadRequestFailedException, IOException {
        File fileForDownload = new File(_filePathOnServer);
        MessageToClient msgDF = new MessageToClient(ClientActionType.DOWNLOAD_FILE, data);
        boolean sent = replyToClient(msgDF);
        if (!sent)
            throw new DownloadRequestFailedException("Failed to initiate download sequence.");

        logger.info("Initiating data send...");

        DataOutputStream dos = new DataOutputStream(clientConnection.getClientSocket().getOutputStream());
        FileInputStream fis = new FileInputStream(fileForDownload);
        bis = new BufferedInputStream(fis);

        byte[] buf = new byte[1024 * 8];
        long bytesToRead = fileForDownload.length();
        int bytesRead;
        while (bytesToRead > 0 && (bytesRead = bis.read(buf, 0, (int) Math.min(buf.length, bytesToRead))) != -1) {
            dos.write(buf, 0, bytesRead);
            bytesToRead -= bytesRead;
        }
        return bis;
    }

    // Informing source (uploader) that file received by user (downloader)
    private void informSrcOfSuccess(Map data) {
        String title = strings.media_ready_title();
        String msg = String.format(strings.media_ready_body(), !destContact.equals("") ? destContact : destId);
        String token = usersDataAccess.getUserRecord(sourceId).getToken();
        boolean sent = pushSender.sendPush(token, PushEventKeys.TRANSFER_SUCCESS, title, msg, data);
        if (!sent)
            logger.warning("Failed to inform user " + sourceId + " of transfer success to user: " + destId);
    }

    private void handleDownloadFailure(Exception e) {

        logger.severe("User " + messageInitiaterId + " download request failed. Exception:" + e.getMessage());

        String title = strings.media_undelivered_title();

        String dest = (!destContact.equals("") ? destContact : destId);
        String msgTransferFailed = String.format(strings.media_undelivered_body(), dest);

        String destHtml = "<b><font color=\"#00FFFF\">" + (!destContact.equals("") ? destContact : destId) + "</font></b>";
        String msgTransferFailedHtml = String.format(strings.media_undelivered_body(), destHtml);

        HashMap<DataKeys, Object> data = new HashMap<>();
        data.put(DataKeys.HTML_STRING, msgTransferFailedHtml);

        // Informing sender that file did not reach destination
        logger.severe("Informing sender:" + sourceId + " that file did not reach destination:" + destId);
        String senderToken = usersDataAccess.getUserRecord(sourceId).getToken();
        boolean sent = pushSender.sendPush(senderToken, PushEventKeys.SHOW_ERROR, title, msgTransferFailed, data);

        if(!sent)
            logger.severe("Failed trying to Inform sender:" + sourceId + " that file did not reach destination:" + destId + ". Empty token");

        // informing destination of request failure
        logger.severe("Informing destination:" + destId + " that download request failed");
        HashMap<DataKeys, Object> replyData = new HashMap();
        replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.DOWNLOAD_FAILURE));
        replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));

        // Marking in communication history record that the transfer has failed
        char FALSE = '0';
        try {
            dao.updateMediaTransferRecord(commId, DAO.COL_TRANSFER_SUCCESS, FALSE);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
    }
}
