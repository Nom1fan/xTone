package actions;

import java.util.Map;

import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.MessageTriggerEventOnly;
import MessagesToServer.ActionType;
import ServerObjects.SmsSender;
import ServerObjects.SmsVerificationAccess;
import utils.RandUtils;

/**
 * Created by Mor on 23/04/2016.
 */
public class ActionGetSmsCode extends Action {

    public static final int MIN = 1000;
    public static final int MAX = 9999;

    public ActionGetSmsCode() {
        super(ActionType.GET_SMS_CODE);
    }

    @Override
    public void doAction(Map data) {

        String _internationePhoneNumber = (String) data.get(DataKeys.INTERNATIONAL_PHONE_NUMBER);

        _logger.info("Generating SMS code for [User]:" + _internationePhoneNumber);

        int code = RandUtils.getRand(MIN,MAX);

        String msg = "Your verification code:" + code;
        SmsSender.sendSms(_internationePhoneNumber, msg);
        boolean isOK = SmsVerificationAccess.instance(_dal).insertSmsVerificationCode(_messageInitiaterId, code);

        if(isOK)
            replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.NO_ACTION_REQUIRED, null, null)));
        else
            replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.GET_SMS_CODE_FAILED, null, null)));
    }
}
