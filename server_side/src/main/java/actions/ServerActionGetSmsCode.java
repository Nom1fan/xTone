package actions;

import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import sms_service.SmsSender;
import com.database.SmsVerificationAccess;
import utils.RandUtils;

/**
 * Created by Mor on 23/04/2016.
 */
public class ServerActionGetSmsCode extends ServerAction {

    public static final int MIN = 1000;
    public static final int MAX = 9999;

    public ServerActionGetSmsCode() {
        super(ServerActionType.GET_SMS_CODE);
    }

    @Override
    public void doAction(Map data) {

        String _internationePhoneNumber = (String) data.get(DataKeys.INTERNATIONAL_PHONE_NUMBER);

        _logger.info("Generating SMS code for [User]:" + _internationePhoneNumber);

        int code = RandUtils.getRand(MIN,MAX);

        String msg = "Your verification code:" + code;
        SmsSender.sendSms(_internationePhoneNumber, msg);
        boolean isOK = SmsVerificationAccess.instance(_dal).insertSmsVerificationCode(_messageInitiaterId, code);

        HashMap replyData = new HashMap();
        if(isOK) {
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.NO_ACTION_REQUIRED, null, null));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
        }
        else {
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.GET_SMS_CODE_FAILED, null ,null));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
        }
    }
}
