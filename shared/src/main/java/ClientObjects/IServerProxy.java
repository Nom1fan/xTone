package ClientObjects;

import FilesManager.FileManager;

public interface IServerProxy {

	public void connect();	
//	public void downloadFileFromServer(TransferDetails td);
	public void uploadFileToServer(final byte[] fileData, final String extension, final FileManager.FileType fileType, final String destNumber, final String fullFilePath);
	public void isLogin(final String phoneNumber);
	public ConnectionToServer getConnectionToServer();		
	public void startHeartBeatThread();	
	
}
