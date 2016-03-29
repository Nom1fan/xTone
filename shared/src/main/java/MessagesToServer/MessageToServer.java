package MessagesToServer;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

import DalObjects.IDAL;
import LogObjects.LogsManager;
import MessagesToClient.MessageToClient;
import ServerObjects.AppMetaAccess;
import ServerObjects.ClientsDataAccess;
import ServerObjects.CommHistoryAccess;
import ServerObjects.ConnectionToClient;

/**
 * Abstract message to the server, containing information of the source client and enables generic interface for server actions corresponding to the message
 * @author Mor
 *
 */
public abstract class MessageToServer implements Serializable  {

	private static final long serialVersionUID = -6478414954653475710L;
    private ConnectionToClient _clientConnection;
    protected IDAL _dal;
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
                _logger.info(_messageInitiaterId + " [Message]:" + msg.getClass().getSimpleName());

                if(_clientConnection == null)
                    throw(new NullPointerException("Could not get client connection for user:"+_messageInitiaterId));

                _clientConnection.sendToClient(msg);
                return true;
            }
            catch (IOException | NullPointerException e)
            {
                _logger.severe("Failed to send reply to [User]:" + _messageInitiaterId +
                                ". [Message]:" + msg.getClass().getSimpleName() +
                                ". [Exception]:" + (e.getMessage()!=null ? e.getMessage() : e));
                e.printStackTrace();

                return false;
            }
    }

	abstract public boolean doServerAction() throws IOException, ClassNotFoundException;
	public final void set_clientConnection(ConnectionToClient cc) { _clientConnection = cc; setId(); }
    public void set_dal(IDAL _dal) { this._dal = _dal; }
    private void setId() {
        _clientConnection.setInfo("id", _messageInitiaterId);
    }
    protected final ConnectionToClient get_clientConnection() {
        return _clientConnection;
    }

}
