package MessagesToClient;

import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
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

	public MessageDownloadFile(TransferDetails td) {
						
		_td = td;		
		_fileName = _td.getSourceWithExtension();		
		_sourceId = _td.getSourceId();
		_myId = _td.getDestinationId();

	}
	
	@Override
	public EventReport doClientAction(ConnectionToServer connectionToServer) throws IOException {
							
		  try
		  {
			  // Creating file and directories for downloaded file
			  File specialCallIncomingDir = new File(SharedConstants.specialCallIncomingPath +_sourceId);
			  specialCallIncomingDir.mkdirs();
			  String fileStoragePath =  specialCallIncomingDir.getAbsolutePath() + "/" + _fileName;
			  File newFile = new File(fileStoragePath);
			  newFile.createNewFile();

			  FileOutputStream fos = new FileOutputStream(fileStoragePath);
			  BufferedOutputStream bos = new BufferedOutputStream(fos);
			  DataInputStream dis = new DataInputStream(connectionToServer.getClientSocket().getInputStream());

			  System.out.println("Reading data...");
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
				  throw new IOException("download was stopped abruptly");

		  }
		  catch(IOException e)
		  {

			e.printStackTrace();
		  	String errMsg = "DOWNLOAD_FAILURE:" + (e.getMessage() != null ? e.getMessage() : "");

			return new EventReport(EventType.DOWNLOAD_FAILURE,errMsg,_fileName);
		  }

				
		  String desc = "DOWNLOAD_SUCCESS. Filename:"+_fileName;
		  return new EventReport(EventType.DOWNLOAD_SUCCESS,desc,_td);
	}

}
