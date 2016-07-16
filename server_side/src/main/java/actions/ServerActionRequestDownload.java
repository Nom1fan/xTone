package actions;

import com.database.CommHistoryAccess;
import com.database.IDAO;
import com.database.UsersDataAccess;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.PushEventKeys;
import DataObjects.SpecialMediaType;
import EventObjects.EventReport;
import EventObjects.EventType;
import Exceptions.DownloadRequestFailedException;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import ServerObjects.ILangStrings;
import annotations.ServerActionAnno;
import lang.StringsFactory;
import pushservice.BatchPushSender;

/**
 * Created by Mor on 23/04/2016.
 */
@ServerActionAnno(actionType = ServerActionType.REQUEST_DOWNLOAD)
public class ServerActionRequestDownload extends ServerAction {

    private ILangStrings strings = StringsFactory.instance().getStrings(ILangStrings.ENGLISH);
    private String _sourceId;
    private String _destId;
    private String _destContact;
    private String _sourceLocale;
    private String _filePathOnServer;
    private SpecialMediaType _specialMediaType;
    private int _commId;

    public ServerActionRequestDownload() {
        super(ServerActionType.REQUEST_DOWNLOAD);
    }

    @Override
    public void doAction(Map data) throws IOException {

        Object oCommId      = data.get(DataKeys.COMM_ID);
        _commId             = oCommId instanceof Double ? ((Double)oCommId).intValue() : (int)oCommId;
        _sourceId           = (String) data.get(DataKeys.SOURCE_ID);
        _destId             = (String) data.get(DataKeys.DESTINATION_ID);
        _destContact        = (String) data.get(DataKeys.DESTINATION_CONTACT_NAME);
        _sourceLocale       = (String) data.get(DataKeys.SOURCE_LOCALE);
        _filePathOnServer   = (String) data.get(DataKeys.FILE_PATH_ON_SERVER);

        if(_sourceLocale!=null)
            strings = StringsFactory.instance().getStrings(_sourceLocale);

        _logger.info(_messageInitiaterId + " is requesting download from:" + _sourceId + ". File path on server:" + _filePathOnServer  + "...");

        BufferedInputStream bis = null;
        try {
            FileManager fileForDownload = new FileManager(_filePathOnServer);
            MessageToClient msgDF = new MessageToClient(ClientActionType.DOWNLOAD_FILE, (HashMap)data);
            boolean sent = replyToClient(msgDF);
            if(!sent)
                throw new DownloadRequestFailedException("Failed to initiate download sequence.");

            _logger.info("Initiating data send...");

            DataOutputStream dos = new DataOutputStream(_clientConnection.getClientSocket().getOutputStream());
            FileInputStream fis = new FileInputStream(fileForDownload.getFile());
            bis = new BufferedInputStream(fis);

            byte[] buf = new byte[1024 * 8];
            long bytesToRead = fileForDownload.getFileSize();
            int bytesRead;
            while (bytesToRead > 0 && (bytesRead = bis.read(buf, 0, (int) Math.min(buf.length, bytesToRead))) != -1) {
                dos.write(buf, 0, bytesRead);
                bytesToRead -= bytesRead;
            }

            // Informing source (uploader) that file received by user (downloader)
            String title = strings.media_ready_title();
            String msg = String.format(strings.media_ready_body(), !_destContact.equals("") ? _destContact : _destId);
            String token = _dao.getUserRecord(_sourceId).get_token();
            sent = BatchPushSender.sendPush(token, PushEventKeys.TRANSFER_SUCCESS, title , msg, data);
            if(!sent)
                _logger.warning("Failed to inform user " + _sourceId + " of transfer success to user: " + _destId);

            // Marking in communication history record that the transfer was successful
            char TRUE = '1';
            CommHistoryAccess.instance(_dao).updateCommunicationRecord(_commId, IDAO.COL_TRANSFER_SUCCESS, TRUE);


        } catch (FileInvalidFormatException |
                FileExceedsMaxSizeException |
                FileDoesNotExistException |
                DownloadRequestFailedException |
                FileMissingExtensionException |
                SQLException e)  {

            handleDownloadFailure(e);

        } catch (IOException e) {

            handleDownloadFailure(e);
            throw e; //In case of IOException we need to notify the server infra
        } finally {
            if(bis!=null)
                bis.close();
        }
    }

    private void handleDownloadFailure(Exception e) {

        _logger.severe("User " + _messageInitiaterId + " download request failed. Exception:" + e.getMessage());

        String title = strings.media_undelivered_title();

        String dest = (!_destContact.equals("") ? _destContact : _destId);
        String msgTransferFailed = String.format(strings.media_undelivered_body(), dest);

        String destHtml = "<b><font color=\"#00FFFF\">" + (!_destContact.equals("") ? _destContact : _destId) + "</font></b>";
        String msgTransferFailedHtml = String.format(strings.media_undelivered_body(), destHtml);

        HashMap<DataKeys,Object> data = new HashMap<>();
        data.put(DataKeys.HTML_STRING, msgTransferFailedHtml);

        // Informing sender that file did not reach destination
        _logger.severe("Informing sender:" + _sourceId + " that file did not reach destination:" + _destId);
        String senderToken = UsersDataAccess.instance(_dao).getUserPushToken(_sourceId);
        if(!senderToken.equals(""))
            BatchPushSender.sendPush(senderToken, PushEventKeys.SHOW_ERROR, title, msgTransferFailed, data);
        else
            _logger.severe("Failed trying to Inform sender:" + _sourceId + " that file did not reach destination:" + _destId + ". Empty token");

        // informing destination of request failure
        _logger.severe("Informing destination:" + _destId + " that download request failed");
        HashMap<DataKeys,Object> replyData = new HashMap();
        replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.DOWNLOAD_FAILURE, null, null));
        replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));

        // Marking in communication history record that the transfer has failed
        char FALSE = '0';
        CommHistoryAccess.instance(_dao).updateCommunicationRecord(_commId, IDAO.COL_TRANSFER_SUCCESS, FALSE);
    }
}
