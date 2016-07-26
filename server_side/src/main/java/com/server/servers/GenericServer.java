package com.server.servers;

import com.server.actions.ActionFactory;
import com.server.actions.ServerAction;
import com.server.database.DAO;
import com.server.lang.ServerConstants;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

import MessagesToServer.MessageToServer;
import ServerObjects.AbstractServer;
import ServerObjects.ConnectionToClient;

/**
 * Created by Mor on 18/12/2015.
 */
public class GenericServer extends AbstractServer {

    private DAO dao;

    private Logger logger;

    private ActionFactory actionFactory;

    private String serverName;

    private int port;

    /**
     * Constructs a new server.
     *
     * @param serverName The name of the server
     * @param port the port number on which to listen.
     */
    public GenericServer(String serverName, int port) {
        super(serverName, port);
        this.serverName = serverName;
        this.port = port;
    }

    public void runServer() {
        try {
            dao.updateAppRecord(ServerConstants.MIN_SUPPORTED_APP_VERSION);

            System.out.println("Starting " + serverName + "...");
            listen();
        }
        catch (IOException | SQLException e) {
            e.printStackTrace();
            logger.severe("Failed to initialize "+serverName+" components. Exception:" + e.getMessage());
            try {
                this.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    //region AbstractServer hook methods
    @Override
    protected void handleMessageFromClient(Object oMsg, ConnectionToClient ctc) {
        MessageToServer msg = (MessageToServer) oMsg;

        try {
            String clientId = msg.get_messageInitiaterId();
            ctc.setInfo("id", clientId);
            ServerAction serverAction = actionFactory.getAction(msg.getActionType());
            serverAction.setMessageInitiaterId(clientId);
            serverAction.setClientConnection(ctc);
            serverAction.verifyUserRegistration();
            serverAction.doAction(msg.getData());
            closeConnectionToClient(ctc);
        }

        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void repeatedlyTryToRestart() {

        while(!isListening()) {

            try {
                listen();
                Thread.sleep(RESTART_INTERVAL);
            } catch (IOException | InterruptedException e)  {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void serverStarted() {

        System.out.println(serverName + " running on port:" + port + "...");
        logger.info(serverName + " running on port:" + port + "...");
        logger.info("Minimum supported app version: " + ServerConstants.MIN_SUPPORTED_APP_VERSION);
    }

    @Override
    protected void serverStopped() {

        logger.severe(serverName + " stopped");

        try {
            close();
            System.out.println("Attempting to restart " + serverName + "...");

            repeatedlyTryToRestart();

        } catch (IOException e) {
            e.printStackTrace();
            logger.severe("Failed to close" + serverName + ". Exception:" + (e.getMessage()!=null ? e.getMessage() : e));
        }
    }

    @Override
    protected  void serverClosed() {

        System.out.println(serverName + " closed");
        logger.severe(serverName + " closed");
    }

    @Override
    protected void listeningException(Throwable exception) {

        exception.printStackTrace();
        String exMsg = exception.getMessage();
        logger.severe("Listening exception:" + (exMsg!=null ? exMsg : exception.toString()));
    }

    @Override
    synchronized protected void clientException(ConnectionToClient client, Throwable exception) {

        exception.printStackTrace();
        String exMsg = exception.getMessage();
        logger.severe("Client " + client.getInfo("id") + " threw an exception:" + (exMsg!=null ? exMsg : exception.toString()));
    }

    @Override
    synchronized protected void clientDisconnected(ConnectionToClient client) {

        //logger.warning("Client " + client.getInfo("id") + " disconnected");
    }

    @Override
    synchronized protected void clientTimedOut(ConnectionToClient client) {

        //logger.warning("Client " + client.getInfo("id") + " timed out. Socket closed.");
    }

    //region Assisting methods
    private void closeConnectionToClient(ConnectionToClient ctc) {

        try {
            ctc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //endregion
    //endregion
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setDao(DAO dao) {
        this.dao = dao;
    }

    public void setActionFactory(ActionFactory actionFactory) {
        this.actionFactory = actionFactory;
    }
}