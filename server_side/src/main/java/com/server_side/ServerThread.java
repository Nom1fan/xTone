package com.server_side;

import java.io.IOException;
import java.util.logging.Logger;
import LogObjects.LogsManager;
import MessagesToServer.MessageToServer;
import ServerObjects.ClientsManager;
import ServerObjects.ConnectionToClient;

/**
 *
 * @author Mor
 * This class is the server worker
 * It is run by a thread per client connection.
 * The thread listens for client messages, and upon receiving them,
 * executes the appropriate action and resumes listening
 *
 */
public class ServerThread extends Thread {

    private ConnectionToClient _ctc;
    private Logger logger = LogsManager.getServerLogger("Server");

    public ServerThread(ConnectionToClient ctc) {

        _ctc = ctc;
    }

    @Override
    public void run() {

        boolean cont = true;
        MessageToServer inputMsg;

        while(cont)
        {
            try
            {

                inputMsg = (MessageToServer) _ctc.getClientMessage();
                if(inputMsg!=null)
                {
                    inputMsg.setClientConnection(_ctc);
                    cont = inputMsg.doServerAction();
                }
            }
            catch(IOException e)
            {
                logger.severe("Error received:"+e.getMessage()+". Shutting down client connection...");
                ClientsManager.removeClientConnection(_ctc);
                cont = false;
            }
            catch (Exception e) {

                logger.severe("Error received:"+e.getMessage()+" Attempting to continue...");
                e.printStackTrace();
            }

        }

    }
}
