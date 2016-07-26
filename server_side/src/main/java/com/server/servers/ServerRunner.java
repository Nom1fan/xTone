package com.server.servers;

/**
 * Created by Mor on 25/07/2016.
 */
public interface ServerRunner {

    void runServer();

    boolean isServerRunning();

    boolean isServerStopped();

    GenericServer getStorageServer();

    GenericServer getLogicServer();

    void stopServer();
}
