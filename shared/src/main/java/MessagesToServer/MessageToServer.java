package MessagesToServer;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;
import LogObjects.LogsManager;
import MessagesToClient.MessageToClient;
import ServerObjects.ClientsManager;
import ServerObjects.ConnectionToClient;

/**
 * Abstract message to the server, containing information of the source client and enables generic interface for server actions corresponding to the message
 * @author Mor
 *
 */
public abstract class MessageToServer implements Serializable  {

	private static final long serialVersionUID = -6478414954653475710L;
    private ConnectionToClient clientConnection;
	protected String _messageInitiaterId;
	protected boolean cont = true;
	protected Logger logger = null;
	
	public MessageToServer(String messageInitiaterId) { _messageInitiaterId = messageInitiaterId; }
	
	protected final void initLogger() { logger = LogsManager.getServerLogger(); }

    /**
     * Send a message reply to the client that initiated the message to server
     * @param msg - The message to send to the client
     * @return - returns 'true' if message was sent successfully, else returns 'false' and removes the client connection from client pool
     */
	protected final boolean replyToClient(MessageToClient msg) {


            try
            {
                logger.info(_messageInitiaterId + " with message:" + msg.getClass().getSimpleName());

                if(clientConnection==null)
                    throw(new NullPointerException("Could not get client connection for user:"+_messageInitiaterId));

                clientConnection.sendToClient(msg);
                return true;
            }
            catch (IOException | NullPointerException e)
            {
                logger.severe("[Failed to send reply to user]:"+_messageInitiaterId+" of message:"+msg.getClass().getSimpleName()+" [Exception]:"+e.getMessage()+ "Terminating client connection...");
                e.printStackTrace();

                return false;
            }

    }

	abstract public boolean doServerAction() throws IOException, ClassNotFoundException;
	public final void setClientConnection(ConnectionToClient cc) { clientConnection = cc; setId(); }
    private void setId() {
        clientConnection.setInfo("id", _messageInitiaterId);
    }
    protected ConnectionToClient getClientConnection() {
        return clientConnection;
    }
		
}
