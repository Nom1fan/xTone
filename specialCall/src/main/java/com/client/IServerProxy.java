package com.client;

import com.model.response.Response;

public interface IServerProxy {

	void handleMessageFromServer(Response msg, ConnectionToServerImpl connectionToServer);
	void handleDisconnection(ConnectionToServerImpl cts, String msg);
}
