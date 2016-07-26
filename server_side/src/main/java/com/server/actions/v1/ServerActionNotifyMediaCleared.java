package com.server.actions.v1;

import com.server.actions.ServerAction;
import com.server.annotations.ServerActionAnno;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.PushEventKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import ServerObjects.LangStrings;

/**
 * Created by Mor on 23/04/2016.
 */
@ServerActionAnno(actionType = ServerActionType.NOTIFY_MEDIA_CLEARED)
public class ServerActionNotifyMediaCleared extends ServerAction {

    public ServerActionNotifyMediaCleared() {
        super(ServerActionType.NOTIFY_MEDIA_CLEARED);
    }

    @Override
    public void doAction(Map data) throws SQLException {

        String clearRequesterId = (String) data.get(DataKeys.SOURCE_ID);
        String clearerId = messageInitiaterId;
        String clearerName = (String) data.get(DataKeys.DESTINATION_CONTACT_NAME);

        logger.info("Informing [Clear media requester]:" + clearRequesterId +
                " that [User]:" + clearerId +
                " cleared his media of [SpecialMediaType]: " + data.get(DataKeys.SPECIAL_MEDIA_TYPE));

        String clearRequesterToken = dao.getUserRecord(clearRequesterId).getToken();

        // Informing clear requester that media was cleared
        String sourceLocale = (String) data.get(DataKeys.SOURCE_LOCALE);
        LangStrings strings;

        if(sourceLocale!=null)
            strings = stringsFactory.getStrings(sourceLocale);
        else
            strings = stringsFactory.getStrings(LangStrings.Languages.ENGLISH.toString());

        String title = strings.media_cleared_title();
        String msgBody = String.format(strings.media_cleared_body(), clearerName!=null && !clearerName.equals("") ? clearerName : clearerId);
        boolean sent = pushSender.sendPush(clearRequesterToken, PushEventKeys.CLEAR_SUCCESS, title, msgBody, data);

        if(!sent) {
            logger.severe("Failed to inform [Clear media requester]:" + clearRequesterId +
                    "that [User]:" + clearerId +
                    " cleared his media of [SpecialMediaType]:" + data.get(DataKeys.SPECIAL_MEDIA_TYPE));
        }

        HashMap<DataKeys, Object> replyData = new HashMap();
        replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.NO_ACTION_REQUIRED, null, null));
        replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
    }
}
