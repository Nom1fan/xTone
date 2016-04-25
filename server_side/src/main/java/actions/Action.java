package actions;

import java.io.IOException;
import java.util.Map;

import DalObjects.IDAL;
import Exceptions.UserUnregisteredException;
import MessagesToClient.MessageToClient;
import MessagesToServer.ActionType;
import ServerObjects.ConnectionToClient;
import ServerObjects.UsersDataAccess;
import log.Logged;

/**
 * Created by Mor on 23/04/2016.
 */
public abstract class Action extends Logged {

    protected ConnectionToClient _clientConnection;
    protected IDAL _dal;
    protected String _messageInitiaterId;
    protected ActionType _actionType;

    public abstract void doAction(Map data) throws IOException;

    public Action(ActionType actionType) {
        super();
        _actionType = actionType;
    }

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

    public void set_clientConnection(ConnectionToClient _clientConnection) {
        this._clientConnection = _clientConnection;
    }

    public void set_dal(IDAL _dal) {
        this._dal = _dal;
    }

    public void set_messageInitiaterId(String _messageInitiaterId) {
        this._messageInitiaterId = _messageInitiaterId;
    }

    public final void verifyUserRegistration() throws UserUnregisteredException {

        if(!(_actionType.equals(ActionType.REGISTER)) && !(_actionType.equals(ActionType.GET_SMS_CODE))) {
            boolean isRegistered = UsersDataAccess.instance(_dal).isRegistered(_messageInitiaterId);
            if(!isRegistered)
                throw new UserUnregisteredException("User " + _messageInitiaterId + " has attempted to perform an action:" + this.getClass() + " but is unregistered");
        }
    }
}
