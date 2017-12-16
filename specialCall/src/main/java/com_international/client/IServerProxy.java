package com_international.client;

import com_international.model.response.Response;

public interface IServerProxy {

	void handleMessageFromServer(Response msg, ConnectionToServerImpl connectionToServer);
	void handleDisconnection(ConnectionToServerImpl cts, String msg);
}
