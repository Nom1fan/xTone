package MessagesToServer;

import java.io.IOException;

import DataObjects.PushEventKeys;
import DataObjects.TransferDetails;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.MessageTriggerEventOnly;
import ServerObjects.BatchPushSender;
import ServerObjects.ClientsDataAccess;

/**
 * Created by Mor on 18/02/2016.
 */
public class MessageNotifyMediaCleared extends MessageToServer {

    private TransferDetails _td;

    public MessageNotifyMediaCleared(String messageInitiaterId, TransferDetails td) {
        super(messageInitiaterId);
        _td = td;
    }

    @Override
    public boolean doServerAction() throws IOException, ClassNotFoundException {

        initLogger();

        String clearRequesterId = _td.getSourceId();
        String clearerId = _messageInitiaterId;

        _logger.info("Informing [Clear media requester]:" + clearRequesterId +
                     " that [User]:" + clearerId +
                     " cleared his media of [SpecialMediaType]: " + _td.getSpMediaType());

        String clearRequesterToken = ClientsDataAccess.instance(_dal).getUserPushToken(clearRequesterId);

        String title = "Media cleared!";
        String msgBody = "Media for user " + clearerId + " is cleared!";
        boolean sent = BatchPushSender.sendPush(clearRequesterToken, PushEventKeys.CLEAR_SUCCESS, title, msgBody, _td);

        if(!sent) {
            _logger.severe("Failed to inform [Clear media requester]:" + clearRequesterId +
                    "that [User]:" + clearerId +
                    " cleared his media of [SpecialMediaType]:" + _td.getSpMediaType());
        }

        replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.NO_ACTION_REQUIRED, null, null)));

        return false;
    }
}
