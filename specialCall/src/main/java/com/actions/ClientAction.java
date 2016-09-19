package com.actions;

import com.client.ConnectionToServer;

import java.io.IOException;

import EventObjects.EventReport;
import MessagesToClient.ClientActionType;

/**
 * Created by Mor on 26/04/2016.
 */
public abstract class ClientAction<DATA_TYPE> {

    protected ClientActionType _clientActionType;
    protected ConnectionToServer _connectionToServer;

    public ClientAction(ClientActionType clientActionType) {
        super();
        _clientActionType = clientActionType;
    }

    abstract public EventReport doClientAction(DATA_TYPE data) throws IOException;

    public void setConnectionToServer(ConnectionToServer connectionToServer) {
        _connectionToServer = connectionToServer;
    }
}

