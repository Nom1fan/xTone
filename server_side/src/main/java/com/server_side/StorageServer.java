package com.server_side;


import java.io.EOFException;
import java.io.IOException;
import java.util.logging.Logger;
import DataObjects.SharedConstants;
import LogObjects.LogsManager;
import MessagesToServer.MessageToServer;
import ServerObjects.AbstractServer;
import ServerObjects.ClientsManager;
import ServerObjects.ConnectionToClient;

/**
 * Created by Mor on 23/09/2015.
 */
public class StorageServer extends GenericServer {

    public StorageServer(String serverName, int port) {
        super(serverName, port);
    }
}
