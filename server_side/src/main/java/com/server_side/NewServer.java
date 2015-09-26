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
public class NewServer extends AbstractServer {

    private ClientsManager _clientsManager;
    private static Logger _logger = null;

    public NewServer() {
        super(SharedConstants.PORT);

        try {
            LogsManager.createServerLogsDir();
            LogsManager.clearLogs();
            _logger = LogsManager.getServerLogger();
            _clientsManager = new ClientsManager();

            System.out.println("Starting server...");

            listen();
        }
        catch (IOException e ) {
            e.printStackTrace();
            _logger.severe("Failed to initialize server components. Exception:" + e.getMessage());
            try {
                this.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

    }

    /* AbstractServer hook methods */

    @Override
    protected void handleMessageFromClient(Object oMsg, ConnectionToClient ctc) {
        MessageToServer msg = (MessageToServer) oMsg;

        try {
            msg.setClientConnection(ctc);
            boolean cont = msg.doServerAction();
            if(!cont)
                closeConnectionToClient(ctc);
        }
        catch(EOFException e) {
            _logger.info("Client closed the connection. logging off client...");
            closeConnectionToClient(ctc);
        }
        catch(IOException e) {
            e.printStackTrace();
            _logger.severe("Error received:" + e.getMessage() + ". Shutting down client connection...");
            closeConnectionToClient(ctc);
        }
        catch (Exception e) {
            e.printStackTrace();
            _logger.severe("Error received:" + e.getMessage() + " Attempting to continue...");
        }
    }

    @Override
    protected void serverStarted() {

        System.out.println("Server running on port:" + SharedConstants.PORT + "...");
        _logger.info("Server running on port:" + SharedConstants.PORT + "...");
    }

    @Override
    protected void serverStopped() {

        System.out.println("Server stopped");
        _logger.info("Server stopped");
    }

    /* Assisting methods */

    private void closeConnectionToClient(ConnectionToClient ctc) {

        ClientsManager.removeClientConnection(ctc);
        try {
            ctc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {

        new NewServer();
    }
}
