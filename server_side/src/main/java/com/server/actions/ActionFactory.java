package com.server.actions;

import Exceptions.NoSuchActionException;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 25/07/2016.
 */
public interface ActionFactory {
    ServerAction getAction(ServerActionType serverActionType) throws NoSuchActionException;
}
