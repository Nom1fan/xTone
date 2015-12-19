package com.server_side;

import DataObjects.SharedConstants;

/**
 * Created by Mor on 16/10/2015.
 */
public class ServerRunner {

    public static void main(String args[]) {

        new LogicServer(LogicServer.class.getSimpleName(), SharedConstants.LOGIC_SERVER_PORT);
        new StorageServer(StorageServer.class.getSimpleName(), SharedConstants.STORAGE_SERVER_PORT);
    }
}
