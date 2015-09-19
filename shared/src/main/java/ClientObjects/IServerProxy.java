package ClientObjects;

import FilesManager.FileManager;

public interface IServerProxy {

//	public void downloadFileFromServer(TransferDetails td);
	public void uploadFileToServer(final String destNumber, final FileManager managedFile);
	public void isLogin(final String phoneNumber);
	public ConnectionToServer getConnectionToServer();
}
