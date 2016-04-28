package com.actions;

import java.io.IOException;
import java.util.Map;

import ClientObjects.ConnectionToServer;
import EventObjects.EventReport;
import MessagesToClient.ClientActionType;

/**
 * Created by Mor on 26/04/2016.
 */
public abstract class ClientAction {

    protected ClientActionType _clientActionType;
    protected ConnectionToServer _connectionToServer;

    public ClientAction(ClientActionType clientActionType) {
        super();
        _clientActionType = clientActionType;
    }

    abstract public EventReport doClientAction(Map data) throws IOException;

    public void setConnectionToServer(ConnectionToServer connectionToServer) {
        _connectionToServer = connectionToServer;
    }
}

