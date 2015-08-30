package MessagesToClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import ClientObjects.ConnectionToServer;
import ClientObjects.IServerProxy;
import DataObjects.SharedConstants;
import DataObjects.TransferDetails;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToServer.MessageTriggerEventForRemoteUser;

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
	public EventReport doClientAction(IServerProxy serverProxy) throws UnknownHostException,
			IOException {
							
		  try 
		  {
				// Creating file and directories for downloaded file
		        File specialCallIncomingDir = new File(SharedConstants.specialCallPath+_sourceId);
				specialCallIncomingDir.mkdirs();			
		        String fileStoragePath =  specialCallIncomingDir.getAbsolutePath() +"/"+ _fileName;
		        File dlFile = new File(fileStoragePath);
		        dlFile.createNewFile();
		        FileOutputStream fos = new FileOutputStream(dlFile);
		        BufferedOutputStream bos = new BufferedOutputStream(fos);	        	        	      
			    		    					
			    // Writing file to disk
			    bos.write(_fileData);
			    bos.flush();
				bos.close();
		  }
		  catch(Exception e)
		  {
			String errMsg;
			e.printStackTrace();
			if(e.getMessage()!=null)
				errMsg = "DOWNLOAD_FAILURE:"+e.getMessage();
			else
				errMsg = "DOWNLOAD_FAILURE";
			return new EventReport(EventType.DOWNLOAD_SUCCESS,errMsg,_fileName);
		  }
		  
		  // Informing source (uploader) that file received by user (downloader)
		  ConnectionToServer cts = serverProxy.getConnectionToServer();
		  String infoMsg = "TRANSFER_SUCCESS: to "+_myId+". Filename:"+_fileName;
		  cts.sendMessage(new MessageTriggerEventForRemoteUser(_myId, _sourceId, 
				  new EventReport(EventType.DISPLAY_MESSAGE, infoMsg, null)));
				
		  String desc = "DOWNLOAD_SUCCESS. Filename:"+_fileName;
		  return new EventReport(EventType.DOWNLOAD_SUCCESS,desc,_fileName);	
	}

}