package MessagesToServer;

import java.io.IOException;

import DataObjects.CallRecord;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.MessageTriggerEventOnly;

/**
 * Created by Mor on 08/03/2016.
 */
public class MessageInsertMediaCallRecord extends MessageToServer {

    private CallRecord _callRecord;

    public MessageInsertMediaCallRecord(String messageInitiaterId, CallRecord callRecord) {
        super(messageInitiaterId);
        this._callRecord = callRecord;
    }

    @Override
    public boolean doServerAction() throws IOException, ClassNotFoundException {

        initLogger();

        _commHistoryManager.insertMediaCallRecord(_callRecord);

        replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.NO_ACTION_REQUIRED, null, null)));

        return false;
    }
}
