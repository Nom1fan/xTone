package MessagesToServer;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import DataObjects.PushEventKeys;
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
		private byte[] _fileData;


		public MessageUploadFile(String srcId, TransferDetails td, byte[] fileData) throws IOException {
			super(srcId);
			_destId = td.getDestinationId();
			_td = td;
			_fileData = fileData;

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
			FileManager.createNewFile(fileFullPath, _fileData);
		} catch (IOException e) {
			e.printStackTrace();
			logger.severe("Upload from user:"+_messageInitiaterId+" to user:"+_destId+" Failed:"+e.getMessage());
			String errMsg = "UPLOAD_FAILED: Server failed to create the file";
			cont = replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.UPLOAD_FAILURE, errMsg, null)));

			return cont;
		}


		// Informing source (uploader) that the file is on the way
		String infoMsg = "File uploaded to server";
		cont = replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.UPLOAD_SUCCESS, infoMsg, null)));
			
		if(!cont)
			return cont;
			
		// Sending file to destination
        _td.set_filePathOnServer(fileFullPath);
		String destToken = ClientsManager.getClientPushToken(_destId);
		String pushEventAction = PushEventKeys.PENDING_DOWNLOAD;
        boolean sent = PushSender.sendPush(destToken, pushEventAction, new Gson().toJson(_td));

		if(!sent)
		{
			String errMsg = "TRANSFER_FAILURE: "+ _destId +" did not receive pending download push for file:"+_td.getSourceWithExtension();
			String initiaterToken = ClientsManager.getClientPushToken(_messageInitiaterId);
			cont = PushSender.sendPush(initiaterToken, PushEventKeys.SHOW_MESSAGE, errMsg);
		}

		return cont;
		
	}
}
