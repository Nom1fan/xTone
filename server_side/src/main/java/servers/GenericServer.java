package servers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

import DalObjects.IDAL;
import LogObjects.LogsManager;
import MessagesToServer.MessageToServer;
import ServerObjects.AbstractServer;
import ServerObjects.ClientsManager;
import ServerObjects.CommHistoryManager;
import ServerObjects.ConnectionToClient;

/**
 * Created by Mor on 18/12/2015.
 */
public class GenericServer extends AbstractServer {

    protected static Logger _logger = null;

    /**
     * Constructs a new server.
     *
     * @param serverName
     * @param port the port number on which to listen.
     */
    public GenericServer(String serverName, int port, IDAL dal) {
        super(serverName, port);
        this.serverName = serverName;
        try {
            LogsManager.createServerLogsDir();
            LogsManager.clearLogs();
            _logger = LogsManager.getServerLogger();

            ClientsManager.initialize(dal);
            CommHistoryManager.initialize(dal);

            System.out.println("Starting " + serverName + "...");

            listen();
        }
        catch (IOException e) {
            e.printStackTrace();
            _logger.severe("Failed to initialize "+serverName+" components. Exception:" + e.getMessage());
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
        _logger.info(serverName + " running on port:" + port + "...");
    }

    @Override
    protected void serverStopped() {

        System.out.println(serverName + "server stopped");
        _logger.severe(serverName + "server stopped");

        try {
            close();
            System.out.println("Attempting to restart " + serverName + "...");

            repeatedlyTryToRestart();

        } catch (IOException e) {
            e.printStackTrace();
            _logger.severe("Failed to close" + serverName + ". Exception:" + (e.getMessage()!=null ? e.getMessage() : e));
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

        exception.printStackTrace();
        String exMsg = exception.getMessage();
        _logger.severe("Client " + client.getInfo("id") + " threw an exception:" + (exMsg!=null ? exMsg : exception.toString()));
    }

    @Override
    synchronized protected void clientDisconnected(ConnectionToClient client) {

        //_logger.warning("Client " + client.getInfo("id") + " disconnected");
    }

    @Override
    synchronized protected void clientTimedOut(ConnectionToClient client) {

        //_logger.warning("Client " + client.getInfo("id") + " timed out. Socket closed.");
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
