package MessagesToServer;

import java.io.IOException;

import DataObjects.PushEventKeys;
import DataObjects.SharedConstants;
import DataObjects.SpecialMediaType;
import DataObjects.TransferDetails;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.MessageTriggerEventOnly;
import ServerObjects.BatchPushSender;

/**
 * Created by Mor on 18/02/2016.
 */
public class MessageClearMedia extends MessageToServer {

    private String _destId;
    private TransferDetails _td;

    public MessageClearMedia(String srcId, TransferDetails td) {
        super(srcId);
        _td = td;
        _destId = _td.getDestinationId();
    }

    @Override
    public boolean doServerAction() throws IOException, ClassNotFoundException {

        initLogger();

        StringBuilder infoMsg = new StringBuilder();
        infoMsg.
                append("Initiating clear media. ").
                append("[Source]:").append(_messageInitiaterId).
                append(". [Destination]:").append(_destId).
                append(". [Special Media Type]:").append(_td.get_spMediaType());

        _logger.info(infoMsg.toString());

        String destToken = _clientsManager.getClientPushToken(_destId);
        String pushEventAction = PushEventKeys.CLEAR_MEDIA;
        boolean sent = BatchPushSender.sendPush(destToken, pushEventAction, _td);

        String msg;
        if(sent)
            msg = "You will be notified when the media is cleared";
        else
            msg = "Oops! Failed to clear media. Please try again later.";

        replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.DISPLAY_MESSAGE, msg, null)));

        return false;
    }
}
