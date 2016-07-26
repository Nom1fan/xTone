package com.server.actions.v1;

import com.server.actions.ServerAction;
import com.server.annotations.ServerActionAnno;

import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 23/04/2016.
 */
@ServerActionAnno(actionType = ServerActionType.IS_REGISTERED)
public class ServerActionIsRegistered extends ServerAction {

    public ServerActionIsRegistered() {
        super(ServerActionType.IS_REGISTERED);
    }

    @Override
    public void doAction(Map data) {

        String id = (String) data.get(DataKeys.DESTINATION_ID);
        logger.info(messageInitiaterId + " is checking if " + id + " is logged in...");
        data.put(DataKeys.IS_REGISTERED, usersDataAccess.isRegistered(id));
        replyToClient(new MessageToClient(ClientActionType.IS_REGISTERED_RES, (HashMap)data));
    }
}
