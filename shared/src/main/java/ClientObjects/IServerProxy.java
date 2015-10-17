package ClientObjects;

import java.io.IOException;

import DataObjects.TransferDetails;
import FilesManager.FileManager;
import MessagesToClient.MessageToClient;

public interface IServerProxy {

	public ConnectionToServer getConnectionToServer();
	public void handleMessageFromServer(MessageToClient msg);
	public void handleDisconnection(String msg);
}
