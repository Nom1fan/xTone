package com.server.actions.v1;

import com.server.actions.ServerAction;
import com.server.annotations.ServerActionAnno;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 23/04/2016.
 */
@ServerActionAnno(actionType = ServerActionType.UNREGISTER)
public class ServerActionUnregister extends ServerAction {

    public ServerActionUnregister() {
        super(ServerActionType.UNREGISTER);
    }

    @Override
    public void doAction(Map data) throws IOException {

        HashMap<DataKeys,Object> replyData = new HashMap();
        replyData.put(DataKeys.IS_UNREGISTER_SUCCESS, true);
        replyToClient(new MessageToClient(ClientActionType.UNREGISTER_RES, replyData)); //TODO remove boolean, unregister always succeeds for user as long as connection exists. Backend operations are for the server only
        String token = (String) data.get(DataKeys.PUSH_TOKEN);
        usersDataAccess.unregisterUser(messageInitiaterId, token);
    }
}