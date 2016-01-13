package MessagesToServer;

import com.google.gson.Gson;
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
import DataObjects.SpecialMediaType;
import DataObjects.TransferDetails;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;
import MessagesToClient.MessageTriggerEventOnly;
import ServerObjects.ClientsManager;
import ServerObjects.CommHistoryManager;
import ServerObjects.PushSender;

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
			logger.info(infoMsg);

			try {

				// Preparing file placeholder
				File newFile = new File(fileFullPath.toString());
				newFile.getParentFile().mkdirs();
				newFile.createNewFile();

				FileOutputStream fos = new FileOutputStream(newFile);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				DataInputStream dis = new DataInputStream(getClientConnection().getClientSocket().getInputStream());

				logger.info("Reading data...");
				byte[] buf = new byte[1024*8];
				long fileSize = _td.getFileSize();
				int bytesRead;
				while (fileSize > 0 && (bytesRead = dis.read(buf, 0, (int)Math.min(buf.length, fileSize))) != -1)
				{
					bos.write(buf,0,bytesRead);
					fileSize -= bytesRead;
				}

				bos.close();

				if(fileSize > 0)
					throw new IOException("Upload was stopped abruptly");
                else if (fileSize < 0 )
                    throw new IOException("Read too many bytes. Upload seems corrupted.");
			}
			catch (IOException e) {
				e.printStackTrace();
				logger.severe("Upload from user:"+_messageInitiaterId+" to user:"+_destId+" Failed:"+e.getMessage());
				String errMsg = "UPLOAD_FAILED:"+e.getMessage();
				cont = replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.UPLOAD_FAILURE, errMsg, null)));
				PushSender.sendPush(ClientsManager.getClientPushToken(_messageInitiaterId), PushEventKeys.SHOW_MESSAGE, errMsg);

				return cont;
			}


			// Informing source (uploader) that the file is on the way
			infoMsg = "File:"+_td.get_fullFilePathSrcSD()+" uploaded to server";
			cont = replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.UPLOAD_SUCCESS, infoMsg, _td)));

			if(!cont) {
				logger.severe("Canceling file upload from "+_td.getSourceId()+" to "+_td.getDestinationId());
				return cont;
			}

			// Inserting the record of the file upload, retrieving back the commId
			int commId = CommHistoryManager.insertCommunicationRecord(
                    _td.get_spMediaType().toString(),
                    _td.getSourceId(),
                    _td.getDestinationId(),
                    _td.getExtension(),
                    (int) _td.getFileSize());

			// Sending file to destination
			_td.set_commId(commId);
			_td.set_filePathOnServer(fileFullPath.toString());
			String destToken = ClientsManager.getClientPushToken(_destId);
			String pushEventAction = PushEventKeys.PENDING_DOWNLOAD;
			boolean sent = PushSender.sendPush(destToken, pushEventAction, new Gson().toJson(_td));

			if(!sent)
			{
				String errMsg = "TRANSFER_FAILURE: "+ _destId +" did not receive pending download push for file:"+_td.getSourceWithExtension();
				String initiaterToken = ClientsManager.getClientPushToken(_messageInitiaterId);
				// Informing source (uploader) that the file was not sent to destination
				cont = PushSender.sendPush(initiaterToken, PushEventKeys.SHOW_MESSAGE, errMsg);
			}

			return cont;
		
	}
}
