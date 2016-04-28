package actions;

import java.util.HashMap;
import java.util.Map;

import DataObjects.CallRecord;
import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import com.database.CommHistoryAccess;

/**
 * Created by Mor on 23/04/2016.
 */
public class ServerActionInsertMediaCallRecord extends ServerAction {

    private CallRecord _callRecord;

    public ServerActionInsertMediaCallRecord() {
        super(ServerActionType.INSERT_MEDIA_CALL_RECORD);
    }

    @Override
    public void doAction(Map data) {

        _callRecord = (CallRecord) data.get(DataKeys.CALL_RECORD);
        CommHistoryAccess.instance(_dal).insertMediaCallRecord(_callRecord);
        HashMap<DataKeys, Object> replyData = new HashMap();
        replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.NO_ACTION_REQUIRED, null, null));
        replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
    }
}
