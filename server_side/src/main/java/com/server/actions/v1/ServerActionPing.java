package com.server.actions.v1;

import com.server.actions.ServerAction;
import com.server.annotations.ServerActionAnno;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 15/07/2016.
 */
@ServerActionAnno(actionType = ServerActionType.PING)
public class ServerActionPing extends ServerAction {
    public ServerActionPing() {
        super(ServerActionType.PING);
    }

    @Override
    public void doAction(Map data) throws IOException, SQLException {
        logger.info("Returning ping to [User]:" + messageInitiaterId);
        MessageToClient msgPong = new MessageToClient(ClientActionType.PING);
        replyToClient(msgPong);
    }

}
