package MessagesToServer;

import java.net.UnknownHostException;
import DataObjects.TransferDetails;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.MessageDownloadFile;
import ServerObjects.ClientsManager;
import FilesManager.FileManager;

public class MessageUploadFile extends MessageToServer {

	private static final long serialVersionUID = 2356276507283427913L;
	private String _destPhone;
	private TransferDetails _td;
	private double _fileSize; // in bytes
	private byte[] _fileData; 	
	
	public MessageUploadFile(String srcId, TransferDetails td, byte[] fileData) {
		super(srcId);
		_td = td;		
		_destPhone = _td.getDestinationId();		
		_fileData = fileData;
		_fileSize = td.getFileSize();
	}

	@Override
	public boolean doServerAction() throws UnknownHostException,
			ClassNotFoundException {
		
		initLogger();
		
		logger.info("Initiating file send from user:"+_messageInitiaterId+" to user:"+_destPhone+"."+" File size:"+FileManager.getFileSizeFormat(_fileSize));
			
		// Informing source (uploader) that the file is on the way
		String infoMsg = "Sending file to:"+_destPhone+"...";
		cont = ClientsManager.sendEventToClient(_messageInitiaterId,new EventReport(EventType.UPLOAD_SUCCESS, infoMsg, _td));
			
		if(!cont)
			return cont;
			
		// Sending file to destination	
		boolean sent = ClientsManager.sendMessageToClient(_destPhone,new MessageDownloadFile(_td, _fileData));
			
		if(!sent)
		{
			String errMsg = "TRANSFER_FAILURE: "+_destPhone+" did not receive upload";
			cont = ClientsManager.sendEventToClient(_messageInitiaterId, new EventReport(EventType.DISPLAY_ERROR, errMsg, null));
		}
		
		return cont;
		
//		*** All the remark zone here is for saving the file to the server. 
//		***	Currently the file is being sent directly to destination without being saved on the server.
//		***	To undo this, remove these remarks
			
//			FileOutputStream fos;
//			BufferedOutputStream bos;
//			try 
//			{		
//				// Creating file to upload
//				Path currentRelativePath = Paths.get("");
//				String path = currentRelativePath.toAbsolutePath().toString();
//				File newFile = new File(path+"\\uploads\\"+_destPhone+"\\"+_messageInitiaterId+"."+_extension);
//				newFile.getParentFile().mkdirs();
//				newFile.createNewFile();
//				fos = new FileOutputStream(newFile);		
//				bos = new BufferedOutputStream(fos);			
//			}	
//			catch(Exception e)
//			{
//				logger.severe("Upload from user:"+_messageInitiaterId+" to user:"+_destPhone+" Failed:"+e.getMessage());		
//				String errMsg = "UPLOAD_FAILED:"+e.getMessage();
//				cont = ClientsManager.informClient(_messageInitiaterId,errMsg, EventType.UPLOAD_FAILURE);
//				
//				return cont;
//			}
//			
//			try 
//			{				 		    		
//			    logger.info("Writing file to disk...");
//			    
//			    // Writing file to disk
//			    bos.write(_file);
//			    bos.flush();
//				bos.close();	
//			}
//			catch(Exception e)
//			{
//				logger.severe("Upload from user:"+_messageInitiaterId+" to user:"+_destPhone+" Failed:"+e.getMessage());			
//				cont = false;
//				return cont;
//			}
		
	}
}
