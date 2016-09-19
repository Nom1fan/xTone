package com.server.actions.v1;

import com.server.actions.ServerAction;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import static MessagesToServer.ServerActionType.PING;

/**
 * Created by Mor on 15/07/2016.
 */
@Component("PING")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ServerActionPing extends ServerAction {
    public ServerActionPing() {
        super(PING);
    }

    @Override
    public void doAction(Map data) throws IOException, SQLException {
        logger.info("Returning ping to [User]:" + messageInitiaterId);
        MessageToClient msgPong = new MessageToClient(ClientActionType.PING);
        replyToClient(msgPong);
    }

}
