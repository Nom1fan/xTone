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
import ServerObjects.LangStrings;

/**
 * Created by Mor on 23/04/2016.
 */
@ServerActionAnno(actionType = ServerActionType.REQUEST_DOWNLOAD)
public class ServerActionRequestDownload extends ServerAction {

    private LangStrings strings;
    private String _sourceId;
    private String _destId;
    private String _destContact;
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
        _sourceId = (String) data.get(DataKeys.SOURCE_ID);
        _destId = (String) data.get(DataKeys.DESTINATION_ID);
        _destContact = (String) data.get(DataKeys.DESTINATION_CONTACT_NAME);
        String _sourceLocale = (String) data.get(DataKeys.SOURCE_LOCALE);
        String filePathOnServer = (String) data.get(DataKeys.FILE_PATH_ON_SERVER);

        if (_sourceLocale != null)
            strings = stringsFactory.getStrings(_sourceLocale);

        logger.info(messageInitiaterId + " is requesting download from:" + _sourceId + ". File path on server:" + filePathOnServer + "...");

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

    private void informSrcOfSuccess(Map data) throws SQLException {
        boolean sent;// Informing source (uploader) that file received by user (downloader)
        String title = strings.media_ready_title();
        String msg = String.format(strings.media_ready_body(), !_destContact.equals("") ? _destContact : _destId);
        String token = dao.getUserRecord(_sourceId).getToken();
        sent = pushSender.sendPush(token, PushEventKeys.TRANSFER_SUCCESS, title, msg, data);
        if (!sent)
            logger.warning("Failed to inform user " + _sourceId + " of transfer success to user: " + _destId);
    }

    private void handleDownloadFailure(Exception e) {

        logger.severe("User " + messageInitiaterId + " download request failed. Exception:" + e.getMessage());

        String title = strings.media_undelivered_title();

        String dest = (!_destContact.equals("") ? _destContact : _destId);
        String msgTransferFailed = String.format(strings.media_undelivered_body(), dest);

        String destHtml = "<b><font color=\"#00FFFF\">" + (!_destContact.equals("") ? _destContact : _destId) + "</font></b>";
        String msgTransferFailedHtml = String.format(strings.media_undelivered_body(), destHtml);

        HashMap<DataKeys, Object> data = new HashMap<>();
        data.put(DataKeys.HTML_STRING, msgTransferFailedHtml);

        // Informing sender that file did not reach destination
        logger.severe("Informing sender:" + _sourceId + " that file did not reach destination:" + _destId);
        String senderToken = usersDataAccess.getUserRecord(_destId).getToken();
        if (!senderToken.equals(""))
            pushSender.sendPush(senderToken, PushEventKeys.SHOW_ERROR, title, msgTransferFailed, data);
        else
            logger.severe("Failed trying to Inform sender:" + _sourceId + " that file did not reach destination:" + _destId + ". Empty token");

        // informing destination of request failure
        logger.severe("Informing destination:" + _destId + " that download request failed");
        HashMap<DataKeys, Object> replyData = new HashMap();
        replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.DOWNLOAD_FAILURE, null, null));
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