package com.server.actions.v1;

import com.server.actions.ServerAction;
import com.server.annotations.ServerActionAnno;

import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.PushEventKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 23/04/2016.
 */
@ServerActionAnno(actionType = ServerActionType.CLEAR_MEDIA)
public class ServerActionClearMedia extends ServerAction {

    public ServerActionClearMedia() {
        super(ServerActionType.CLEAR_MEDIA);
    }

    @Override
    public void doAction(Map data) {

        logger.info("Initiating clear media. " + data);

        String destId = (String) data.get(DataKeys.DESTINATION_ID);
        String destToken = usersDataAccess.getUserRecord(destId).getToken();
        String pushEventAction = PushEventKeys.CLEAR_MEDIA;
        boolean sent = pushSender.sendPush(destToken, pushEventAction, data);

        HashMap replyData = new HashMap();
        if(sent) {
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.CLEAR_SENT));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
        }
        else {
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.CLEAR_FAILURE));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
        }
    }
}