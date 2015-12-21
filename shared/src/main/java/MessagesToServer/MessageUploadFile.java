package MessagesToServer;

import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
import ServerObjects.ClientsManager;
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
		
		logger.info("Initiating file upload from user:" + _messageInitiaterId + ". Destination user:" + _destId + "." + " File size:" + FileManager.getFileSizeFormat(_td.getFileSize()));
		Path currentRelativePath = Paths.get("");
		String workingDir = currentRelativePath.toAbsolutePath().toString();
		String fileFullPath = workingDir+FileManager.UPLOAD_FOLDER+_destId+"\\"+_td.getSourceWithExtension();

		try {

			// Preparing file placeholder
			File newFile = new File(fileFullPath);
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
		}
	 	catch (IOException e) {
			e.printStackTrace();
			logger.severe("Upload from user:"+_messageInitiaterId+" to user:"+_destId+" Failed:"+e.getMessage());
			String errMsg = "UPLOAD_FAILED: Server failed to create the file";
			cont = replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.UPLOAD_FAILURE, errMsg, null)));

			return cont;
		}


		// Informing source (uploader) that the file is on the way
		String infoMsg = "File:"+_td.get_fullFilePathSrcSD()+" uploaded to server";
		cont = replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.UPLOAD_SUCCESS, infoMsg, null)));
			
		if(!cont) {
			logger.severe("Canceling file upload from "+_td.getSourceId()+" to "+_td.getDestinationId());
			return cont;
		}
			
		// Sending file to destination
        _td.set_filePathOnServer(fileFullPath);
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

		ClientsManager.insertCommunicationRecord(_td.getSourceId(), _td.getDestinationId(), _td.getExtension(), (int)_td.getFileSize());

		return cont;
		
	}
}
