package MessagesToServer;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import DataObjects.PushEventKeys;
import DataObjects.SharedConstants;
import DataObjects.TransferDetails;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;
import MessagesToClient.MessageTriggerEventOnly;
import ServerObjects.BatchPushSender;

public class MessageUploadFile extends MessageToServer {

	private static final long serialVersionUID = 2356276507283427913L;
	private String _destId;
	private TransferDetails _td;

		public MessageUploadFile(String srcId, TransferDetails td) {
			super(srcId);
			_destId = td.getDestinationId();
			_td = td;

		}

		@Override
		public boolean doServerAction() throws UnknownHostException,
				ClassNotFoundException {
		
			initLogger();

			StringBuilder fileFullPath = new StringBuilder();
			Path currentRelativePath = Paths.get("");
			fileFullPath.append(currentRelativePath.toAbsolutePath().toString()); // Working directory

			switch(_td.get_spMediaType())
			{
				case CALLER_MEDIA:
					fileFullPath.append(SharedConstants.UPLOAD_FOLDER).append(_destId).append("\\").    // Destination upload folder
                    append(SharedConstants.CALLER_MEDIA_FOLDER).append(_td.getSourceWithExtension()); // Caller media for destination
					break;
				case PROFILE_MEDIA:
					fileFullPath.append(SharedConstants.UPLOAD_FOLDER).append(_destId).append("\\").    // Destination upload folder
                    append(SharedConstants.PROFILE_MEDIA_RECEIVED_FOLDER).append(_td.getSourceWithExtension()); // Profile media for destination
					break;
				case MY_DEFAULT_PROFILE_MEDIA:
					fileFullPath.append(SharedConstants.UPLOAD_FOLDER).append(_messageInitiaterId).append("\\"). // Source upload folder
                    append(SharedConstants.MY_DEFAULT_PROFILE_MEDIA_FOLDER). // Default profile media folder
                    append(SharedConstants.MY_DEFAULT_PROFILE_MEDIA_FILENAME).
                    append(_td.getExtension());
					break;
				case MY_DEFAULT_CALLER_MEDIA:
					throw new UnsupportedOperationException("Not yet implemented");
					
			}

			String infoMsg = "Initiating file upload. [Source]:" + _messageInitiaterId +
					". [Destination]:" + _destId + "." +
					" [Special Media Type]:" + _td.get_spMediaType() +
					" [File size]:" +
					FileManager.getFileSizeFormat(_td.getFileSize());
			_logger.info(infoMsg);

			BufferedOutputStream bos = null;
			try {

				// Preparing file placeholder
				File newFile = new File(fileFullPath.toString());
				newFile.getParentFile().mkdirs();
				newFile.createNewFile();

				FileOutputStream fos = new FileOutputStream(newFile);
				bos = new BufferedOutputStream(fos);
				DataInputStream dis = new DataInputStream(get_clientConnection().getClientSocket().getInputStream());

				_logger.info("Reading data...");
				byte[] buf = new byte[1024*8];
				long fileSize = _td.getFileSize();
				int bytesRead;
				while (fileSize > 0 && (bytesRead = dis.read(buf, 0, (int)Math.min(buf.length, fileSize))) != -1)
				{
					bos.write(buf,0,bytesRead);
					fileSize -= bytesRead;
				}

				if(fileSize > 0)
					throw new IOException("Upload was stopped abruptly");
                else if (fileSize < 0 )
                    throw new IOException("Read too many bytes. Upload seems corrupted.");
			}
			catch (IOException e) {
				e.printStackTrace();
				_logger.severe("Upload from user:"+_messageInitiaterId+" to user:"+_destId+" Failed:"+e.getMessage());
				String title = "Oops!";
				String errMsg = "Your media to "+_destId+ "was lost on the way! Please try again.";
				String token = _clientsManager.getClientPushToken(_messageInitiaterId);
				//_cont = replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.UPLOAD_FAILURE, errMsg, null)));
				BatchPushSender.sendPush(token , PushEventKeys.SHOW_MESSAGE, title, errMsg);

				return _cont;
			}
			finally {
				if(bos!=null)
					try {
						bos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}

			// Informing source (uploader) that the file is on the way
			infoMsg = "File uploaded";
			replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.UPLOAD_SUCCESS, infoMsg, _td)));

			// Inserting the record of the file upload, retrieving back the commId
			int commId = _commHistoryManager.insertCommunicationRecord(
                    _td.get_spMediaType().toString(),
                    _td.getSourceId(),
                    _td.getDestinationId(),
                    _td.getExtension(),
                    (int) _td.getFileSize());

			// Sending file to destination
			_td.set_commId(commId);
			_td.set_filePathOnServer(fileFullPath.toString());
			String destToken = _clientsManager.getClientPushToken(_destId);
			String pushEventAction = PushEventKeys.PENDING_DOWNLOAD;
			boolean sent = BatchPushSender.sendPush(destToken, pushEventAction, _td);

			if(!sent)
			{
				String title = "Media undelivered";
				String errMsg = "Oops! "+ _destId +" did not receive your media! Try again.";
				String initiaterToken = _clientsManager.getClientPushToken(_messageInitiaterId);
				// Informing source (uploader) that the file was not sent to destination
				BatchPushSender.sendPush(initiaterToken, PushEventKeys.SHOW_MESSAGE, title, errMsg);
			}

			return _cont;
		
	}
}
