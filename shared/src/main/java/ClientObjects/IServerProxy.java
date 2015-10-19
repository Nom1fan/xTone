package ClientObjects;

import java.io.IOException;
import java.sql.Connection;

import DataObjects.TransferDetails;
import FilesManager.FileManager;
import MessagesToClient.MessageToClient;

public interface IServerProxy {

	public void handleMessageFromServer(MessageToClient msg, ConnectionToServer connectionToServer);
	public void handleDisconnection(String msg);
}
