package actions;

import com.database.SmsVerificationAccess;

import java.util.Map;

import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import annotations.ServerActionAnno;

/**
 * Created by Mor on 23/04/2016.
 */
@ServerActionAnno(actionType = ServerActionType.GET_SMS_CODE_FOR_LOAD_TEST)
        public class ServerActionGetSmsCodeForLoadTest extends ServerAction {

    public ServerActionGetSmsCodeForLoadTest() {
        super(ServerActionType.GET_SMS_CODE_FOR_LOAD_TEST);
    }

    @Override
    public void doAction(Map data) {

        _logger.info("Generating SMS code for [User]:" + _messageInitiaterId);
        int code = 1111;
        boolean isOK = SmsVerificationAccess.instance(_dao).insertSmsVerificationCode(_messageInitiaterId, code);

        if(isOK) {
            _replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.GET_SMS_CODE_SUCCESS));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, _replyData));
        }
        else {
            _replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.GET_SMS_CODE_FAILURE));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, _replyData));
        }
    }
}
