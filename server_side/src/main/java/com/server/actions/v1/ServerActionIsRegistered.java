package com.server.actions.v1;

import com.server.actions.ServerAction;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 23/04/2016.
 */
@Component("IS_REGISTERED")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
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
