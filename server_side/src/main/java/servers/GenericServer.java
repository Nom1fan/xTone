package servers;

import com.database.MySqlDAL;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.util.logging.Logger;

import DataObjects.SharedConstants;
import LogObjects.LogsManager;
import MessagesToServer.MessageToServer;
import ServerObjects.AbstractServer;
import ServerObjects.AppMetaManager;
import ServerObjects.ClientsManager;
import ServerObjects.CommHistoryManager;
import ServerObjects.ConnectionToClient;
import data_objects.ServerConstants;

/**
 * Created by Mor on 18/12/2015.
 */
public class GenericServer extends AbstractServer {

    protected static Logger _logger = null;

    static {
        try {
            LogsManager.createServerLogsDir();
            LogsManager.clearLogs();
        } catch (IOException e) {
            e.printStackTrace();
        }

        _logger = LogsManager.get_serverLogger();
    }

    /**
     * Constructs a new server.
     *
     * @param serverName
     * @param port the port number on which to listen.
     */
    public GenericServer(String serverName, int port) {
        super(serverName, port);
        this.serverName = serverName;
        try {
            new MySqlDAL().updateAppRecord(SharedConstants.APP_VERSION, ServerConstants.LAST_SUPPORTED_APP_VERSION);

            System.out.println("Starting " + serverName + "...");
            listen();
        }
        catch (IOException | SQLException e) {
            e.printStackTrace();
            _logger.severe("Failed to initialize "+serverName+" components. Exception:" + e.getMessage());
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
            msg.set_clientConnection(ctc);
            //TODO Develop a more generic way to pass managers to messages
            msg.set_clientsManager(new ClientsManager(new MySqlDAL()));
            msg.set_commHistoryManager(new CommHistoryManager(new MySqlDAL()));
            msg.set_appMetaManager(new AppMetaManager(new MySqlDAL()));
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
            } catch (IOException e)  {
                e.printStackTrace();
            } catch (InterruptedException e)  {
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
    //endregion

    //region Assisting methods
    private void closeConnectionToClient(ConnectionToClient ctc) {

        //ClientsManager.removeClientConnection(ctc);
        try {
            ctc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //endregion

}
