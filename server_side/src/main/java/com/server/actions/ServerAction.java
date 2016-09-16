package com.server.actions;

import com.server.annotations.ServerActionAnno;
import com.server.database.DAO;
import com.server.database.SmsVerificationAccess;
import com.server.database.UserDataAccess;
import com.server.lang.StringsFactory;
import com.server.pushservice.PushSender;
import com.server.sms_service.SmsSender;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import DataObjects.DataKeys;
import Exceptions.UserUnregisteredException;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import ServerObjects.ConnectionToClient;

/**
 * Created by Mor on 23/04/2016.
 */
@ServerActionAnno(actionType = ServerActionType.ABSTRACT)
public abstract class ServerAction {

    @Autowired
    protected PushSender pushSender;

    @Autowired
    protected SmsSender smsSender;

    @Autowired
    protected DAO dao;

    @Autowired
    protected UserDataAccess usersDataAccess;

    @Autowired
    protected SmsVerificationAccess smsVerificationAccess;

    @Autowired
    protected Logger logger;

    @Autowired
    protected StringsFactory stringsFactory;

    protected ConnectionToClient clientConnection;
    protected String messageInitiaterId;
    protected final HashMap<DataKeys, Object> replyData;
    private final ServerActionType serverActionType;
    private final List<ServerActionType> preRegistrationActions = new LinkedList() {{
        add(ServerActionType.REGISTER);
        add(ServerActionType.GET_SMS_CODE);
        add(ServerActionType.GET_SMS_CODE_FOR_LOAD_TEST);
        add(ServerActionType.PING);
    }};

    public ServerAction(ServerActionType serverActionType) {
        super();
        this.serverActionType = serverActionType;
        replyData = new HashMap<>();
    }

    public abstract void doAction(Map data) throws IOException, SQLException;

    /**
     * Send a message reply to the client that initiated the message to server
     *
     * @param msg - The message to send to the client
     * @return - returns 'true' if message was sent successfully. Otherwise, returns 'false'.
     */
    protected final boolean replyToClient(MessageToClient msg) {

        try {
            logger.info(messageInitiaterId + " [Message]:" + msg.getActionType() + " [Data]:" + msg.getResult());

            if (clientConnection == null)
                throw (new NullPointerException("Could not get client connection for user:" + messageInitiaterId));

            clientConnection.sendToClient(msg);
            return true;
        } catch (IOException | NullPointerException e) {
            logger.severe("Failed to send reply to [User]:" + messageInitiaterId +
                    ". [Message]:" + msg.getClass().getSimpleName() +
                    ". [Exception]:" + (e.getMessage() != null ? e.getMessage() : e));
            e.printStackTrace();

            return false;
        }
    }

    public void setClientConnection(ConnectionToClient clientConnection) {
        this.clientConnection = clientConnection;
    }

    public void setMessageInitiaterId(String messageInitiaterId) {
        this.messageInitiaterId = messageInitiaterId;
    }

    public ServerActionType getServerActionType() {
        return serverActionType;
    }

    public final void verifyUserRegistration() throws UserUnregisteredException {

        if (!isPreRegistrationAction()) {
            boolean isRegistered = usersDataAccess.isRegistered(messageInitiaterId);
            if (!isRegistered)
                throw new UserUnregisteredException("User " + messageInitiaterId + " has attempted to perform an action:" + serverActionType + " but is unregistered");
        }
    }

    private boolean isPreRegistrationAction() {
        return
                preRegistrationActions.contains(serverActionType);
    }


}
