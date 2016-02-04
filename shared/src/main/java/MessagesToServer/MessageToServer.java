package MessagesToServer;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;
import LogObjects.LogsManager;
import MessagesToClient.MessageToClient;
import ServerObjects.ClientsManager;
import ServerObjects.CommHistoryManager;
import ServerObjects.ConnectionToClient;

/**
 * Abstract message to the server, containing information of the source client and enables generic interface for server actions corresponding to the message
 * @author Mor
 *
 */
public abstract class MessageToServer implements Serializable  {

	private static final long serialVersionUID = -6478414954653475710L;
    private ConnectionToClient _clientConnection;
    protected ClientsManager _clientsManager;
    protected CommHistoryManager _commHistoryManager;
	protected String _messageInitiaterId;
	protected boolean _cont = false;
	protected Logger _logger = null;
	
	public MessageToServer(String messageInitiaterId) { _messageInitiaterId = messageInitiaterId; }
	
	protected final void initLogger() { _logger = LogsManager.get_serverLogger(); }

    /**
     * Send a message reply to the client that initiated the message to server
     * @param msg - The message to send to the client
     * @return - returns 'true' if message was sent successfully. Otherwise, returns 'false'.
     */
	protected final boolean replyToClient(MessageToClient msg) {

            try
            {
                _logger.info(_messageInitiaterId + " with message:" + msg.getClass().getSimpleName());

                if(_clientConnection == null)
                    throw(new NullPointerException("Could not get client connection for user:"+_messageInitiaterId));

                _clientConnection.sendToClient(msg);
                return true;
            }
            catch (IOException | NullPointerException e)
            {
                _logger.severe("Failed to send reply to [user]:" + _messageInitiaterId +
                                " of [message]:" + msg.getClass().getSimpleName() +
                                ". Received [Exception]:" + (e.getMessage()!=null ? e.getMessage() : e) + ".");
                e.printStackTrace();

                return false;
            }
    }

	abstract public boolean doServerAction() throws IOException, ClassNotFoundException;
	public final void set_clientConnection(ConnectionToClient cc) { _clientConnection = cc; setId(); }
    public final void set_clientsManager(ClientsManager clientsManager) { _clientsManager = clientsManager; }
    public final void set_commHistoryManager (CommHistoryManager commHistoryManager) { _commHistoryManager = commHistoryManager; }
    private void setId() {
        _clientConnection.setInfo("id", _messageInitiaterId);
    }
    protected final ConnectionToClient get_clientConnection() {
        return _clientConnection;
    }
		
}
