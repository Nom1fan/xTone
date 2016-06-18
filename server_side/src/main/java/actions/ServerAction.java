package actions;

import com.database.IDAO;
import com.database.UsersDataAccess;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import Exceptions.UserUnregisteredException;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import ServerObjects.ConnectionToClient;
import log.Logged;

/**
 * Created by Mor on 23/04/2016.
 */
public abstract class ServerAction extends Logged {

    protected ConnectionToClient _clientConnection;
    protected IDAO _dal;
    protected String _messageInitiaterId;
    protected ServerActionType _serverActionType;
    protected HashMap<DataKeys, Object> _replyData;

    public abstract void doAction(Map data) throws IOException;

    public ServerAction(ServerActionType serverActionType) {
        super();
        _serverActionType = serverActionType;
        _replyData = new HashMap<>();
    }

    /**
     * Send a message reply to the client that initiated the message to server
     * @param msg - The message to send to the client
     * @return - returns 'true' if message was sent successfully. Otherwise, returns 'false'.
     */
    protected final boolean replyToClient(MessageToClient msg) {

        try
        {
            _logger.info(_messageInitiaterId + " [Message]:" + msg.getActionType() + " [Data]:" + msg.getData());

            if(_clientConnection == null)
                throw(new NullPointerException("Could not get client connection for user:" + _messageInitiaterId));

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

    public void set_clientConnection(ConnectionToClient clientConnection) {
        _clientConnection = clientConnection;
    }

    public void set_dal(IDAO _dal) {
        this._dal = _dal;
    }

    public void set_messageInitiaterId(String messageInitiaterId) {
        _messageInitiaterId = messageInitiaterId;
    }

    public final void verifyUserRegistration() throws UserUnregisteredException {

        if(!(_serverActionType.equals(ServerActionType.REGISTER)) && !(_serverActionType.equals(ServerActionType.GET_SMS_CODE))) {
            boolean isRegistered = UsersDataAccess.instance(_dal).isRegistered(_messageInitiaterId);
            if(!isRegistered)
                throw new UserUnregisteredException("User " + _messageInitiaterId + " has attempted to perform an action:" + _serverActionType + " but is unregistered");
        }
    }
}
