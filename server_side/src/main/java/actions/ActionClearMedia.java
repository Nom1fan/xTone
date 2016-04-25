package actions;

import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.PushEventKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.MessageTriggerEventOnly;
import MessagesToServer.ActionType;
import ServerObjects.BatchPushSender;
import ServerObjects.UsersDataAccess;

/**
 * Created by Mor on 23/04/2016.
 */
public class ActionClearMedia extends Action {

    public ActionClearMedia() {
        super(ActionType.CLEAR_MEDIA);
    }

    @Override
    public void doAction(Map data) {

        _logger.info("Initiating clear media. " + data);

        String destId = (String) data.get(DataKeys.DESTINATION_ID);
        String destToken = UsersDataAccess.instance(_dal).getUserPushToken(destId);
        String pushEventAction = PushEventKeys.CLEAR_MEDIA;
        boolean sent = BatchPushSender.sendPush(destToken, pushEventAction, data);

        String msg;
        if(sent)
            replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.NO_ACTION_REQUIRED, null, null)));
        else {
            replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.CLEAR_FAILURE, null, null))); // TODO add support for this in BackgroundBroadcastReceiver
        }
    }
}
