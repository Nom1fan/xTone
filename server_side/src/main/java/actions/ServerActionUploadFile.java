package actions;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.PushEventKeys;
import DataObjects.SharedConstants;
import DataObjects.SpecialMediaType;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import pushservice.BatchPushSender;
import com.database.CommHistoryAccess;
import ServerObjects.ILangStrings;
import com.database.UsersDataAccess;
import lang.StringsFactory;

/**
 * Created by Mor on 23/04/2016.
 */
public class ServerActionUploadFile extends ServerAction {

    public ServerActionUploadFile() {
        super(ServerActionType.UPLOAD_FILE);
    }

    @Override
    public void doAction(Map data) throws IOException {

        StringBuilder fileFullPath = new StringBuilder();
        Path currentRelativePath = Paths.get("");

        // Working directory
        fileFullPath.append(currentRelativePath.toAbsolutePath().toString());

        FileManager managedFile = (FileManager) data.get(DataKeys.MANAGED_FILE);
        String srcWithExtension = _messageInitiaterId + "." + managedFile.getFileExtension();
        String destId = (String) data.get(DataKeys.DESTINATION_ID);
        SpecialMediaType specialMediaType = (SpecialMediaType) data.get(DataKeys.SPECIAL_MEDIA_TYPE);

        switch (specialMediaType) {
            case CALLER_MEDIA:
                // Caller Media is saved in the destination's caller media folder,
                fileFullPath.append(SharedConstants.UPLOAD_FOLDER).append(destId).append("\\").
                        append(SharedConstants.CALLER_MEDIA_FOLDER).append(srcWithExtension);
                break;
            case PROFILE_MEDIA:
                fileFullPath.append(SharedConstants.UPLOAD_FOLDER).append(destId).append("\\").
                        append(SharedConstants.PROFILE_MEDIA_RECEIVED_FOLDER).append(srcWithExtension);
                break;
            case MY_DEFAULT_PROFILE_MEDIA:
                fileFullPath.append(SharedConstants.UPLOAD_FOLDER).append(_messageInitiaterId).append("\\").
                        append(SharedConstants.MY_DEFAULT_PROFILE_MEDIA_FOLDER).
                        append(SharedConstants.MY_DEFAULT_PROFILE_MEDIA_FILENAME).
                        append(managedFile.getFileExtension());
                break;
            case MY_DEFAULT_CALLER_MEDIA:
                throw new UnsupportedOperationException("Not yet implemented");

            default:
                throw new UnsupportedOperationException("ERROR: Invalid special media type:" + specialMediaType);

        }

        String infoMsg = "Initiating file upload. [Source]:" + _messageInitiaterId +
                ". [Destination]:" + destId + "." +
                " [Special Media Type]:" + specialMediaType +
                " [File size]:" +
                FileManager.getFileSizeFormat(managedFile.getFileSize());
        _logger.info(infoMsg);

        BufferedOutputStream bos = null;
        try {

            // Preparing file placeholder
            File newFile = new File(fileFullPath.toString());
            newFile.getParentFile().mkdirs();
            newFile.createNewFile();

            FileOutputStream fos = new FileOutputStream(newFile);
            bos = new BufferedOutputStream(fos);
            DataInputStream dis = new DataInputStream(_clientConnection.getClientSocket().getInputStream());

            _logger.info("Reading data...");
            byte[] buf = new byte[1024 * 8];
            long fileSize = managedFile.getFileSize();
            int bytesRead;
            while (fileSize > 0 && (bytesRead = dis.read(buf, 0, (int) Math.min(buf.length, fileSize))) != -1) {
                bos.write(buf, 0, bytesRead);
                fileSize -= bytesRead;
            }

            if (fileSize > 0)
                throw new IOException("Upload was stopped abruptly");
            else if (fileSize < 0)
                throw new IOException("Read too many bytes. Upload seems corrupted.");

        } catch (IOException e) {
            e.printStackTrace();
            _logger.severe("Upload from [Source]:" + _messageInitiaterId + " to [Destination]:" + destId + " Failed. [Exception]:" + e.getMessage());
            String title = "Oops!";
            String errMsg = "Your media to " + destId + " was lost on the way! Please try again.";
            String token = UsersDataAccess.instance(_dal).getUserPushToken(_messageInitiaterId);
            //_cont = replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.STORAGE_ACTION_FAILURE, errMsg, null)));
            BatchPushSender.sendPush(token, PushEventKeys.SHOW_ERROR, title, errMsg);

        } finally {
            if (bos != null)
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        // Informing source (uploader) that the file is on the way
        HashMap<DataKeys, Object> replyData = new HashMap();
        replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.UPLOAD_SUCCESS, null, data));
        replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));

        // Inserting the record of the file upload, retrieving back the commId
        int commId = CommHistoryAccess.instance(_dal).insertMediaTransferRecord(data);
        _logger.info("commId returned:" + commId);

        // Sending file to destination
        data.put(DataKeys.COMM_ID, commId);
        data.put(DataKeys.FILE_PATH_ON_SERVER, fileFullPath.toString());
        String destToken = UsersDataAccess.instance(_dal).getUserPushToken(destId);
        String pushEventAction = PushEventKeys.PENDING_DOWNLOAD;
        boolean sent = BatchPushSender.sendPush(destToken, pushEventAction, data);

        if (!sent) {
            ILangStrings strings = StringsFactory.instance().getStrings((String)data.get(DataKeys.SOURCE_LOCALE));
            String title = strings.media_undelivered_title();
            String errMsg = strings.media_undelivered_body();
            String initiaterToken = UsersDataAccess.instance(_dal).getUserPushToken(_messageInitiaterId);
            // Informing source (uploader) that the file was not sent to destination
            BatchPushSender.sendPush(initiaterToken, PushEventKeys.SHOW_ERROR, title, errMsg);
        }

    }
}
