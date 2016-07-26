package com.server.actions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Exceptions.NoSuchActionException;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 23/04/2016.
 */
@Component
public class ActionFactoryImpl implements ActionFactory {

    private Map<ServerActionType, ServerAction> actionType2Action = new HashMap<>();

    @Autowired
    public void initMap(List<ServerAction> serverActions) {
        for (ServerAction serverAction : serverActions) {
            actionType2Action.put(serverAction.getServerActionType(), serverAction);
        }
    }

    @Override
    public ServerAction getAction(ServerActionType serverActionType) throws NoSuchActionException {
        ServerAction serverAction = actionType2Action.get(serverActionType);
        if(serverAction == null)
            throw new NoSuchActionException("No such server action: " + serverActionType);
        return serverAction;
    }
}
