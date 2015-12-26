package MessagesToServer;

import java.io.IOException;
import java.sql.SQLException;

import DalObjects.IDAL;
import DataObjects.PushEventKeys;
import DataObjects.TransferDetails;
import EventObjects.EventReport;
import EventObjects.EventType;
import Exceptions.DownloadRequestFailedException;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;
import MessagesToClient.MessageDownloadFile;
import MessagesToClient.MessageTriggerEventOnly;
import ServerObjects.ClientsManager;
import ServerObjects.CommHistoryManager;
import ServerObjects.PushSender;

/**
 * Created by Mor on 09/10/2015.
 */
public class MessageRequestDownload extends MessageToServer {

    private String  _filePathOnServer;
    private TransferDetails _td;

    public MessageRequestDownload(TransferDetails td) {

        super(td.getDestinationId());
        _td = td;
        _filePathOnServer = _td.get_filePathOnServer();

    }

    @Override
    public boolean doServerAction() throws IOException, ClassNotFoundException {

        initLogger();

        logger.info(_messageInitiaterId + " is requesting download from:"+_td.getSourceId()+" of file type:"+_td.getExtension()+"...");

        try {
            FileManager managedFile = new FileManager(_filePathOnServer);
            MessageDownloadFile msgDF = new MessageDownloadFile(_td, managedFile.getFileData());
            boolean sent = replyToClient(msgDF);
            if(!sent)
                throw new DownloadRequestFailedException();

            // Marking in communication history record that the transfer was successful
            char TRUE = '1';
            CommHistoryManager.updateCommunicationRecord(_td.get_commId(), IDAL.COL_TRANSFER_SUCCESS, TRUE);

        } catch (FileInvalidFormatException  |
                 FileExceedsMaxSizeException |
                 FileDoesNotExistException   |
                 DownloadRequestFailedException |
                 FileMissingExtensionException e) {

            e.printStackTrace();
            String msgTransferFailed ="TRANSFER_FAILED: "+_td.getDestinationId()+" did not receive file";

            // Informing sender that file did not reach destination
            logger.severe("Informing sender:"+_td.getSourceId()+" that file did not reach destination:"+_td.getDestinationId());
            String senderId = _td.getSourceId();
            String senderToken = ClientsManager.getClientPushToken(senderId);
            if(!senderToken.equals(""))
                PushSender.sendPush(senderToken, PushEventKeys.SHOW_MESSAGE, msgTransferFailed);
            else
                logger.severe("Failed trying to Inform sender:"+_td.getSourceId()+" that file did not reach destination:"+_td.getDestinationId()+". Empty token");

            // informing destination of request failure
            String msgDownloadFailed = "DOWNLOAD_FAILURE: File send from user:"+_td.getSourceId()+" failed.";
            logger.severe("Informing destination:"+_td.getDestinationId()+" that download request failed");
            replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.DOWNLOAD_FAILURE, msgDownloadFailed, null)));

            // Marking in communication history record that the transfer has failed
            char FALSE = '0';
            CommHistoryManager.updateCommunicationRecord(_td.get_commId(), IDAL.COL_TRANSFER_SUCCESS, FALSE);
        }

        return true;
    }
}
