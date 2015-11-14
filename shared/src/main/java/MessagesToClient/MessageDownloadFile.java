package MessagesToClient;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;

import ClientObjects.ConnectionToServer;
import DataObjects.PushEventKeys;
import DataObjects.SharedConstants;
import DataObjects.TransferDetails;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;
import MessagesToServer.MessageSendPushToRemoteUser;

public class MessageDownloadFile extends MessageToClient {

	private static final long serialVersionUID = -5843761837634526608L;	
	private TransferDetails _td;
	private String _fileName;	
	private String _sourceId;
	private String _myId;
	private byte[] _fileData;
	
	
	public MessageDownloadFile(TransferDetails td, byte[] fileData) {
						
		_td = td;		
		_fileName = _td.getSourceWithExtension();		
		_sourceId = _td.getSourceId();
		_myId = _td.getDestinationId();
		_fileData = fileData;

	}
	
	@Override
	public EventReport doClientAction(ConnectionToServer connectionToServer) throws IOException {
							
		  try
		  {
			// Creating file and directories for downloaded file
			File specialCallIncomingDir = new File(SharedConstants.specialCallIncomingPath +_sourceId);
			specialCallIncomingDir.mkdirs();
			String fileStoragePath =  specialCallIncomingDir.getAbsolutePath() +"/"+ _fileName;
			FileManager.createNewFile(fileStoragePath,_fileData);
		  }
		  catch(IOException e)
		  {
			String errMsg;
			e.printStackTrace();
			if(e.getMessage()!=null)
				errMsg = "DOWNLOAD_FAILURE:"+e.getMessage();
			else
				errMsg = "DOWNLOAD_FAILURE";

			return new EventReport(EventType.DOWNLOAD_FAILURE,errMsg,_fileName);
		  }
		  
		  // Informing source (uploader) that file received by user (downloader)
          String msg = "TRANSFER_SUCCESS: to "+_td.getDestinationId()+". Filename:"+new File(_td.get_fullFilePathSrcSD()).getName();
		  connectionToServer.sendToServer(new MessageSendPushToRemoteUser(_myId, _sourceId, PushEventKeys.TRANSFER_SUCCESS, msg , new Gson().toJson(_td)));
				
		  String desc = "DOWNLOAD_SUCCESS. Filename:"+_fileName;
		  return new EventReport(EventType.DOWNLOAD_SUCCESS,desc,_td);
	}

}
