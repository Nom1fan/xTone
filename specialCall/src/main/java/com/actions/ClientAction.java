package com.actions;

import com.client.ConnectionToServer;
import com.event.EventReport;
import com.model.response.ClientActionType;

import java.io.IOException;

/**
 * Created by Mor on 26/04/2016.
 */
public abstract class ClientAction<RESPONSE> {

    protected ClientActionType _clientActionType;
    protected ConnectionToServer _connectionToServer;

    public ClientAction(ClientActionType clientActionType) {
        super();
        _clientActionType = clientActionType;
    }

    abstract public EventReport doClientAction(RESPONSE response) throws IOException;

    public void setConnectionToServer(ConnectionToServer connectionToServer) {
        _connectionToServer = connectionToServer;
    }
}

