package ClientObjects;

import MessagesToClient.MessageToClient;

public interface IServerProxy {

	void handleMessageFromServer(MessageToClient msg, ConnectionToServer connectionToServer);
	void handleDisconnection(ConnectionToServer cts, String msg);
}
