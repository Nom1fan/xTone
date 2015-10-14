package ClientObjects;


import MessagesToClient.MessageToClient;

public class ConnectionToServer extends AbstractClient {


	private IServerProxy _serverProxy;

	/**
	 * Constructs the client.
	 *
	 * @param host the server's host name.
	 * @param port the port number.
	 */
	public ConnectionToServer(String host, int port, IServerProxy serverProxy) {
		super(host, port);

		_serverProxy = serverProxy;

	}

	@Override
	protected void handleMessageFromServer(Object msg) {

		_serverProxy.handleMessageFromServer((MessageToClient)msg);
	}
}
