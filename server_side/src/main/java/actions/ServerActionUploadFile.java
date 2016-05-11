package actions;

import com.database.CommHistoryAccess;
import com.database.UsersDataAccess;

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
import ServerObjects.ILangStrings;
import lang.StringsFactory;
import pushservice.BatchPushSender;

/**
 * Created by Mor on 23/04/2016.
 */
public class ServerActionUploadFile extends ServerAction {

    private String _destId;
    private String _destContact;
    private HashMap _data;

    public ServerActionUploadFile() {
        super(ServerActionType.UPLOAD_FILE);
    }

    @Override
    public void doAction(Map data) throws IOException {

        _data = (HashMap) data;
        StringBuilder fileFullPath = new StringBuilder();
        Path currentRelativePath = Paths.get("");

        // Working directory
        fileFullPath.append(currentRelativePath.toAbsolutePath().toString());

        FileManager managedFile = (FileManager) _data.get(DataKeys.MANAGED_FILE);
        String srcWithExtension = _messageInitiaterId + "." + managedFile.getFileExtension();
        _destId = (String) _data.get(DataKeys.DESTINATION_ID);
        _destContact = (String) data.get(DataKeys.DESTINATION_CONTACT_NAME);
        SpecialMediaType specialMediaType = (SpecialMediaType) _data.get(DataKeys.SPECIAL_MEDIA_TYPE);

        switch (specialMediaType) {
            case CALLER_MEDIA:
                // Caller Media is saved in the destination's caller media folder,
                fileFullPath.append(SharedConstants.UPLOAD_FOLDER).append(_destId).append("\\").
                        append(SharedConstants.CALLER_MEDIA_FOLDER).append(srcWithExtension);
                break;
            case PROFILE_MEDIA:
                fileFullPath.append(SharedConstants.UPLOAD_FOLDER).append(_destId).append("\\").
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
                ". [Destination]:" + _destId + "." +
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


            // Informing source (uploader) that the file is on the way
            HashMap<DataKeys, Object> replyData = new HashMap();
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.UPLOAD_SUCCESS, null, _data));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));

            // Inserting the record of the file upload, retrieving back the commId
            int commId = CommHistoryAccess.instance(_dal).insertMediaTransferRecord(_data);
            _logger.info("commId returned:" + commId);

            // Sending file to destination
            _data.put(DataKeys.COMM_ID, commId);
            _data.put(DataKeys.FILE_PATH_ON_SERVER, fileFullPath.toString());
            String destToken = UsersDataAccess.instance(_dal).getUserPushToken(_destId);
            String pushEventAction = PushEventKeys.PENDING_DOWNLOAD;
            boolean sent = BatchPushSender.sendPush(destToken, pushEventAction, _data);

            if (!sent) {
                sendMediaUndeliveredMsgToUploader();
            }
        } catch (IOException e) {
            e.printStackTrace();
            sendMediaUndeliveredMsgToUploader();

        } finally {
            if (bos != null)
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

    }

    private void sendMediaUndeliveredMsgToUploader() {

        _logger.severe("Upload from [Source]:" + _messageInitiaterId + " to [Destination]:" + _destId + " Failed.");

        ILangStrings strings = StringsFactory.instance().getStrings((String) _data.get(DataKeys.SOURCE_LOCALE));
        String title = strings.media_undelivered_title();
        String dest = "<b><font color=\"#00FFFF\">" + (!_destContact.equals("") ? _destContact : _destId) + "</font></b>";
        String errMsg = String.format(strings.media_undelivered_body(), dest);
        String initiaterToken = UsersDataAccess.instance(_dal).getUserPushToken(_messageInitiaterId);
        // Informing source (uploader) that the file was not sent to destination
        BatchPushSender.sendPush(initiaterToken, PushEventKeys.SHOW_ERROR, title, errMsg);
    }
}
