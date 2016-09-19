package com.server.actions;

import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 19/09/2016.
 */
public interface ServerActionFactory {
    ServerAction getServerAction(ServerActionType serverActionType);
}
