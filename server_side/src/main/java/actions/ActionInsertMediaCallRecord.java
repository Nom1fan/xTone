package actions;

import java.util.Map;

import DataObjects.CallRecord;
import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.MessageTriggerEventOnly;
import MessagesToServer.ActionType;
import ServerObjects.CommHistoryAccess;

/**
 * Created by Mor on 23/04/2016.
 */
public class ActionInsertMediaCallRecord extends Action {

    private CallRecord _callRecord;

    public ActionInsertMediaCallRecord() {
        super(ActionType.INSERT_MEDIA_CALL_RECORD);
    }

    @Override
    public void doAction(Map data) {

        _callRecord = (CallRecord) data.get(DataKeys.CALL_RECORD);
        CommHistoryAccess.instance(_dal).insertMediaCallRecord(_callRecord);
        replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.NO_ACTION_REQUIRED, null, null)));
    }
}
