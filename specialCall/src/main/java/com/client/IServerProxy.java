package com.client;

import com.model.response.Response;

public interface IServerProxy {

	void handleMessageFromServer(Response msg, ConnectionToServer connectionToServer);
	void handleDisconnection(ConnectionToServer cts, String msg);
}
