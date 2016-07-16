package actions;

import com.database.SmsVerificationAccess;

import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import annotations.ServerActionAnno;
import sms_service.SmsSender;
import utils.RandUtils;

/**
 * Created by Mor on 23/04/2016.
 */
@ServerActionAnno(actionType = ServerActionType.GET_SMS_CODE)
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

        //TODO use this after fixing SmsSender to send hebrew
        //ILangStrings strings = StringsFactory.instance().getStrings(data.get(DataKeys.SOURCE_LOCALE).toString());
        //String msg = String.format(strings.your_verification_code(), code);

        String msg = "Your verification code:" + code;

        boolean isOK = SmsVerificationAccess.instance(_dao).insertSmsVerificationCode(_messageInitiaterId, code);

        HashMap replyData = new HashMap();
        if(isOK) {
            SmsSender.sendSms(_internationePhoneNumber, msg);
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.GET_SMS_CODE_SUCCESS));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
        }
        else {
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.GET_SMS_CODE_FAILURE));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
        }
    }
}
