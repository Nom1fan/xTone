package MessagesToServer;

import com.google.gson.Gson;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
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

        _logger.info(_messageInitiaterId + " is requesting download from:"+_td.getSourceId()+" of file type:"+_td.getExtension()+"...");

        BufferedInputStream bis = null;
        DataOutputStream dos = null;
        try {
            FileManager fileForDownload = new FileManager(_filePathOnServer);
            MessageDownloadFile msgDF = new MessageDownloadFile(_td);
            boolean sent = replyToClient(msgDF);
            if(!sent)
                throw new DownloadRequestFailedException("Failed to initiate download sequence.");

            _logger.info("Initiating data send...");

            dos = new DataOutputStream(get_clientConnection().getClientSocket().getOutputStream());
            FileInputStream fis = new FileInputStream(fileForDownload.getFile());
            bis = new BufferedInputStream(fis);

            byte[] buf = new byte[1024 * 8];
            long bytesToRead = fileForDownload.getFileSize();
            int bytesRead;
            while (bytesToRead > 0 && (bytesRead = bis.read(buf, 0, (int) Math.min(buf.length, bytesToRead))) != -1) {
                dos.write(buf, 0, bytesRead);
//                String msg = "File upload to:"+_td.getDestinationId()+" %.0f%% complete";
//                float percent = (float) (fileSize - bytesToRead) / fileSize * 100;
                bytesToRead -= bytesRead;
            }

            // Informing source (uploader) that file received by user (downloader)
            String msg = "Media for "+_td.getDestinationId()+ " is ready!";
            sent = PushSender.sendPush(_clientsManager.getClientPushToken(_td.getSourceId()), PushEventKeys.TRANSFER_SUCCESS, msg, new Gson().toJson(_td));
            if(!sent)
                _logger.warning("Failed to inform user " + _td.getSourceId() + " of transfer success to user: " + _td.getDestinationId());

            // Marking in communication history record that the transfer was successful
            char TRUE = '1';
            _commHistoryManager.updateCommunicationRecord(_td.get_commId(), IDAL.COL_TRANSFER_SUCCESS, TRUE);


        } catch (FileInvalidFormatException  |
                 FileExceedsMaxSizeException |
                 FileDoesNotExistException   |
                 DownloadRequestFailedException |
                 FileMissingExtensionException e) {

            handleDownloadFailure(e);

        } catch (IOException e) {

            handleDownloadFailure(e);
            throw e; //In case of IOException we need to notify the server infra
        } finally {
        	if(bis!=null)
        		bis.close();
        	if(dos!=null)
        		dos.close();
        }


        return true;
    }

    private void handleDownloadFailure(Exception e) {

        _logger.severe("User " + _messageInitiaterId + " download request failed. Exception:" + e.getMessage());

        String msgTransferFailed ="TRANSFER_FAILED: "+_td.getDestinationId()+" did not receive file";

        // Informing sender that file did not reach destination
        _logger.severe("Informing sender:"+_td.getSourceId()+" that file did not reach destination:"+_td.getDestinationId());
        String senderId = _td.getSourceId();
        String senderToken = _clientsManager.getClientPushToken(senderId);
        if(!senderToken.equals(""))
            PushSender.sendPush(senderToken, PushEventKeys.SHOW_MESSAGE, msgTransferFailed);
        else
            _logger.severe("Failed trying to Inform sender:"+_td.getSourceId()+" that file did not reach destination:"+_td.getDestinationId()+". Empty token");

        // informing destination of request failure
        String msgDownloadFailed = "DOWNLOAD_FAILURE: File send from user:"+_td.getSourceId()+" failed.";
        _logger.severe("Informing destination:"+_td.getDestinationId()+" that download request failed");
        replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.DOWNLOAD_FAILURE, msgDownloadFailed, null)));

        // Marking in communication history record that the transfer has failed
        char FALSE = '0';
        _commHistoryManager.updateCommunicationRecord(_td.get_commId(), IDAL.COL_TRANSFER_SUCCESS, FALSE);
    }

}
