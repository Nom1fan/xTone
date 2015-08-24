package com.server_side;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

import com.sun.org.apache.bcel.internal.classfile.Constant;
import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import DataObjects.SharedConstants;
import LogObjects.LogsManager;
import ServerObjects.ClientsManager;
import ServerObjects.ConnectionToClient;

public class Server {

    private ClientsManager clientsManager;
    private static Logger logger = null;

    public Server() throws IOException {

        LogsManager.clearLogs();
        logger = LogsManager.getServerLogger("Server");
        logger.info("Starting server...");
        System.out.println("Starting server...");

        ServerSocket serverSocket = new ServerSocket(SharedConstants.PORT);

        logger.info("Server running on port:"+SharedConstants.PORT+"...");
        System.out.println("Server running on port:"+SharedConstants.PORT+"...");

        clientsManager = new ClientsManager();

        while(true)
        {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Receiving client connection...");
            ConnectionToClient ctc = new ConnectionToClient(clientSocket);
            new ServerThread(ctc).start();
        }
    }


    public static void main(String[] args) {

        try {
            new Server();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}

