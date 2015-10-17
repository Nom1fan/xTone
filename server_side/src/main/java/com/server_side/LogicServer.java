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
public class LogicServer extends AbstractServer {

    private static Logger _logger = null;

    public LogicServer() {
        super(SharedConstants.LOGIC_SERVER_PORT);

        try {
            LogsManager.createServerLogsDir();
            LogsManager.clearLogs();
            _logger = LogsManager.getServerLogger();
            ClientsManager.initialize();

            System.out.println("Starting logic server...");

            listen();
        }
        catch (IOException e ) {
            e.printStackTrace();
            _logger.severe("Failed to initialize logic server components. Exception:" + e.getMessage());
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
            _logger.severe("Error received:"+e.getStackTrace());
        }
    }

    @Override
    protected void repeatedlyTryToRestart() {
        new Thread() {

            @Override
            public void run() {

                LogicServer restartedLogicServer = new LogicServer();
                while(!restartedLogicServer.isListening()) {
                    restartedLogicServer = new LogicServer();
                    try {
                        Thread.sleep(RESTART_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    @Override
    protected void serverStarted() {

        System.out.println("Logic server running on port:" + SharedConstants.LOGIC_SERVER_PORT + "...");
        _logger.info("Logic server running on port:" + SharedConstants.LOGIC_SERVER_PORT + "...");
    }

    @Override
    protected void serverStopped() {

        System.out.println("Logic server stopped");
        _logger.severe("Logic server stopped");

        System.out.println("Attempting to restart logic server...");

        repeatedlyTryToRestart();
    }

    @Override
    protected void listeningException(Throwable exception) {

        exception.printStackTrace();
        _logger.severe("Listening exception:" + exception.getMessage());
    }

    @Override
    synchronized protected void clientException(ConnectionToClient client, Throwable exception) {

        _logger.severe("Client " + client.getInfo("id") + " threw an exception:" + exception.getMessage());
    }

    @Override
    synchronized protected void clientDisconnected(ConnectionToClient client) {

        _logger.warning("Client " + client.getInfo("id") + " disconnected");
    }

    /* Assisting methods */

    private void closeConnectionToClient(ConnectionToClient ctc) {

        //ClientsManager.removeClientConnection(ctc);
        try {
            ctc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
