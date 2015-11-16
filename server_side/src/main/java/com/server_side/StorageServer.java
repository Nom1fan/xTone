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
public class StorageServer extends AbstractServer {

    private static Logger _logger = null;

    public StorageServer() {
        super(StorageServer.class.getSimpleName(), SharedConstants.STORAGE_SERVER_PORT);

        try {
            LogsManager.createServerLogsDir();
            LogsManager.clearLogs();
            _logger = LogsManager.getServerLogger();
            //ClientsManager.initialize();

            System.out.println("Starting storage server...");

            listen();
        }
        catch (IOException e ) {
            e.printStackTrace();
            _logger.severe("Failed to initialize storage server components. Exception:" + e.getMessage());
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
//        catch(EOFException e) {
//            _logger.info("Client closed the connection in mid-action. logging off client...");
//            closeConnectionToClient(ctc);
//        }
//        catch(IOException e) {
//            e.printStackTrace();
//            _logger.severe("Error received in mid-action:" + e.getMessage() + ". Shutting down client connection...");
//            closeConnectionToClient(ctc);
//        }
        catch (Exception e) {
            e.printStackTrace();
//            _logger.severe("Error received in mid-action:"+e.getStackTrace());
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void repeatedlyTryToRestart() {
        new Thread() {

            @Override
            public void run() {

                StorageServer restartedStorageServer = new StorageServer();
                while(!restartedStorageServer.isListening())
                    restartedStorageServer = new StorageServer();
                try {
                    Thread.sleep(RESTART_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    @Override
    protected void serverStarted() {

        System.out.println("Storage server running on port:" + SharedConstants.STORAGE_SERVER_PORT + "...");
        _logger.info("Storage server running on port:" + SharedConstants.STORAGE_SERVER_PORT + "...");
    }

    @Override
    protected void serverStopped() {

        System.out.println("Storage server stopped");
        _logger.severe("Storage server stopped");

        try {
            close();
            System.out.println("Attempting to restart storage server...");

            repeatedlyTryToRestart();

        } catch (IOException e) {
            e.printStackTrace();
            _logger.severe("Failed to close storage server:"+e.getMessage());
        }
    }

    @Override
    protected void listeningException(Throwable exception) {

        exception.printStackTrace();
        String exMsg = exception.getMessage();
        _logger.severe("Listening exception:" + (exMsg!=null ? exMsg : exception.toString()));
    }

    @Override
    synchronized protected void clientException(ConnectionToClient client, Throwable exception) {

        _logger.severe("Client " + client.getInfo("id") + " threw an exception:" + exception.getMessage());
    }

    @Override
    synchronized protected void clientDisconnected(ConnectionToClient client) {

//        _logger.warning("Client " + client.getInfo("id") + " disconnected");
    }

    @Override
    synchronized protected void clientTimedOut(ConnectionToClient client) {

       // _logger.warning("Client " + client.getInfo("id") + " timed out. Socket closed.");
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
