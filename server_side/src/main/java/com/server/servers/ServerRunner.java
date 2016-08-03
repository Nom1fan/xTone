package com.server.servers;

import java.util.Observer;

/**
 * Created by Mor on 25/07/2016.
 */
public interface ServerRunner {

    void runServer();

    boolean isServerRunning();

    GenericServer getStorageServer();

    GenericServer getLogicServer();

    void stopServer();

    //region Observable methods
    void addObserver(Observer observer);

    void deleteObserver(Observer observer);
    //endregion
}
