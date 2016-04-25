package MessagesToClient;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import ClientObjects.ConnectionToServer;
import DataObjects.DataKeys;
import DataObjects.SharedConstants;
import DataObjects.SpecialMediaType;
import EventObjects.EventReport;
import EventObjects.EventType;

public class MessageDownloadFile extends MessageToClient {

	private static final long serialVersionUID = -5843761837634526608L;	
	private Map _data;
	private String _fileName;	
	private String _sourceId;


	public MessageDownloadFile(Map data) {

		_data = data;
		_fileName = (String) _data.get(DataKeys.SOURCE_WITH_EXTENSION);
		_sourceId = (String) _data.get(DataKeys.SOURCE_ID);
	}
	
	@Override
	public EventReport doClientAction(ConnectionToServer connectionToServer) throws IOException {

		  BufferedOutputStream bos = null;
		  try
		  {
              File folderPath = null;

              switch(SpecialMediaType.valueOf(_data.get(DataKeys.SPECIAL_MEDIA_TYPE).toString()))
              {
                  case CALLER_MEDIA:
                      folderPath = new File(SharedConstants.INCOMING_FOLDER + _sourceId);
                      break;
                  case PROFILE_MEDIA:
                      folderPath = new File(SharedConstants.OUTGOING_FOLDER + _sourceId);
                      break;
                  default:
                      throw new UnsupportedOperationException("Not yet implemented");

              }

			  // Creating file and directories for downloaded file
              folderPath.mkdirs();
			  String fileStoragePath =  folderPath.getAbsolutePath() + "/" + _fileName;
			  File newFile = new File(fileStoragePath);
			  newFile.createNewFile();

			  FileOutputStream fos = new FileOutputStream(fileStoragePath);
			  bos = new BufferedOutputStream(fos);
			  DataInputStream dis = new DataInputStream(connectionToServer.getClientSocket().getInputStream());

			  System.out.println("Reading _data...");
			  byte[] buf = new byte[1024*8];
			  long fileSize = ((Double)_data.get(DataKeys.FILE_SIZE)).longValue();
			  int bytesRead;
			  while (fileSize > 0 && (bytesRead = dis.read(buf, 0, (int)Math.min(buf.length, fileSize))) != -1)
			  {
				  bos.write(buf,0,bytesRead);
				  fileSize -= bytesRead;
			  }

			  if(fileSize > 0)
				  throw new IOException("download was stopped abruptly");

		  }
		  finally {
			  if(bos!=null)
			  	bos.close();
		  }

		  String desc = "DOWNLOAD_SUCCESS. Filename:"+_fileName;
		  return new EventReport(EventType.DOWNLOAD_SUCCESS, desc, _data);
	}

}
