package MessagesToServer;

import java.io.IOException;

import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.MessageTriggerEventOnly;
import ServerObjects.SmsSender;
import ServerObjects.SmsVerificationAccess;
import utils.RandUtils;

/**
 * Created by Mor on 28/03/2016.
 */
public class MessageGetSmsCode extends MessageToServer {

    private static final int MIN = 1000;
    private static final int MAX = 9999;
    private String _internationePhoneNumber;

    public MessageGetSmsCode(String messageInitiaterId, String internationalPhoneNumber) {
        super(messageInitiaterId);
        _internationePhoneNumber = internationalPhoneNumber;
    }

    @Override
    public boolean doServerAction() throws IOException, ClassNotFoundException {

        initLogger();

        _logger.info("Generating SMS code for [User]:" + _internationePhoneNumber);

        int code = RandUtils.getRand(MIN,MAX);

        String msg = "Your verification code:" + code;
        SmsSender.sendSms(_internationePhoneNumber, msg);
        boolean isOK = SmsVerificationAccess.instance(_dal).insertSmsVerificationCode(_messageInitiaterId, code);

        if(isOK)
            replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.NO_ACTION_REQUIRED, null, null)));
        else
            replyToClient(new MessageTriggerEventOnly(new EventReport(EventType.GET_SMS_CODE_FAILED, null, null)));

        return false;
    }
}
