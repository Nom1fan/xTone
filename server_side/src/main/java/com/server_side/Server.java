package com.server_side;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;
import DataObjects.SharedConstants;
import LogObjects.LogsManager;
import ServerObjects.ClientsManager;
import ServerObjects.ConnectionToClient;

public class Server {

    private ClientsManager _clientsManager;
    private static Logger _logger = null;
    private static int _countFailures = 0;
    private static final int MAX_FAILURES = 20;
    private ServerSocket _serverSocket;

    public Server()  {

        try {
            LogsManager.createServerLogsDir();
            LogsManager.clearLogs();
            _logger = LogsManager.getServerLogger("Server");
            _clientsManager = new ClientsManager();

            System.out.println("Starting server...");
            _serverSocket = new ServerSocket(SharedConstants.PORT);
            System.out.println("Server running on port:" + SharedConstants.PORT + "...");
            _logger.info("Server running on port:" + SharedConstants.PORT + "...");

            listen();
        }
        catch (IOException e ) {
            e.printStackTrace();
            _logger.severe("Failed to initialize server components. Exception:"+e.getMessage());
        }

    }

    private void listen() {

        /* Listen loop */
        while(_countFailures<MAX_FAILURES)
        {
            try {

                Socket clientSocket = _serverSocket.accept();
                System.out.println("Receiving client connection...");
                ConnectionToClient ctc = new ConnectionToClient(clientSocket);
                new ServerThread(ctc).start();
            }
            catch(IOException e) {
                _countFailures++;
                e.printStackTrace();
                _logger.severe("Failed to receive client connection. # of Failures:" + _countFailures + " Exception:" + e.getMessage());
            }
        }
        _logger.severe("Server terminating after "+_countFailures+" failures");
    }


    public static void main(String[] args) {

       new Server();
    }
}

