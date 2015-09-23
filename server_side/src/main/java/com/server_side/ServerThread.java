//package com.server_side;
//
//import java.io.EOFException;
//import java.io.IOException;
//import java.util.logging.Logger;
//import LogObjects.LogsManager;
//import MessagesToServer.MessageToServer;
//import ServerObjects.ClientsManager;
//import ServerObjects.SocketWrapper;
//
///**
// *
// * @author Mor
// * This class is the server worker
// * It is run by a thread per client connection.
// * The thread listens for client messages, and upon receiving them,
// * executes the appropriate action and resumes listening
// *
// */
//public class ServerThread extends Thread {
//
//    private SocketWrapper _ctc;
//    private Logger logger = LogsManager.getServerLogger();
//
//    public ServerThread(SocketWrapper ctc) {
//
//        _ctc = ctc;
//    }
//
//    @Override
//    public void run() {
//
//        boolean cont = true;
//        MessageToServer inputMsg;
//
//        while(cont)
//        {
//            try
//            {
//
//                inputMsg = _ctc.getClientMessage();
//                if(inputMsg!=null)
//                {
//                    inputMsg.setClientConnection(_ctc);
//                    cont = inputMsg.doServerAction();
//                }
//            }
//            catch(EOFException e)
//            {
//                logger.info("Client closed the connection. logging off client...");
//                ClientsManager.removeClientConnection(_ctc);
//                cont = false;
//            }
//            catch(IOException e)
//            {
//                logger.severe("Error received:"+e.getMessage()+". Shutting down client connection...");
//                ClientsManager.removeClientConnection(_ctc);
//                cont = false;
//            }
//            catch (Exception e) {
//
//                logger.severe("Error received:"+e.getMessage()+" Attempting to continue...");
//                e.printStackTrace();
//            }
//
//        }
//
//    }
//}
