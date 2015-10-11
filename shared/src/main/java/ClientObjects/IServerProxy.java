package ClientObjects;

import DataObjects.TransferDetails;
import FilesManager.FileManager;

public interface IServerProxy {

	public void requestDownloadFromServer(TransferDetails td);
	public void uploadFileToServer(final String destNumber, final FileManager managedFile);
	public void isRegistered(final String phoneNumber);
	public ConnectionToServer getConnectionToServer();
}
