package actions;

import com.database.UsersDataAccess;

import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.PushEventKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import pushservice.BatchPushSender;

/**
 * Created by Mor on 23/04/2016.
 */
public class ServerActionClearMedia extends ServerAction {

    public ServerActionClearMedia() {
        super(ServerActionType.CLEAR_MEDIA);
    }

    @Override
    public void doAction(Map data) {

        _logger.info("Initiating clear media. " + data);

        String destId = (String) data.get(DataKeys.DESTINATION_ID);
        String destToken = UsersDataAccess.instance(_dal).getUserPushToken(destId);
        String pushEventAction = PushEventKeys.CLEAR_MEDIA;
        boolean sent = BatchPushSender.sendPush(destToken, pushEventAction, data);

        HashMap replyData = new HashMap();
        if(sent) {
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.NO_ACTION_REQUIRED, null, null));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
        }
        else {
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.CLEAR_FAILURE, null, null));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData)); // TODO add support for this in BackgroundBroadcastReceiver
        }
    }
}
