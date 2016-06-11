package ClientObjects;

import MessagesToClient.MessageToClient;

public interface IServerProxy {

	public void handleMessageFromServer(MessageToClient msg, ConnectionToServer connectionToServer);
	public void handleDisconnection(ConnectionToServer cts, String msg);
}
