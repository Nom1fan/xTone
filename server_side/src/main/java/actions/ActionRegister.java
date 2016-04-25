package actions;

import java.util.Map;

import DataObjects.DataKeys;
import MessagesToClient.MessageRegisterRes;
import MessagesToServer.ActionType;
import ServerObjects.SmsVerificationAccess;
import ServerObjects.UsersDataAccess;

/**
 * Created by Mor on 23/04/2016.
 */
public class ActionRegister extends Action {

    public ActionRegister() {
        super(ActionType.REGISTER);
    }

    @Override
    public void doAction(Map data) {

        _logger.info(_messageInitiaterId + " is attempting to register...");

        int smsCode = (int) data.get(DataKeys.SMS_CODE);
        String pushToken = (String) data.get(DataKeys.PUSH_TOKEN);

        int expectedSmsCode = SmsVerificationAccess.instance(_dal).getSmsVerificationCode(_messageInitiaterId);

        MessageRegisterRes msgReply;
        if(smsCode!=SmsVerificationAccess.NO_SMS_CODE && smsCode == expectedSmsCode) {
            boolean isOK = UsersDataAccess.instance(_dal).registerUser(_messageInitiaterId, pushToken);
            msgReply = new MessageRegisterRes(isOK);

        } else {
            _logger.warning("Rejecting registration for [User]:" + _messageInitiaterId +
                    ". [Expected smsCode]:" + expectedSmsCode + " [Received smsCode]:" + smsCode);
            msgReply = new MessageRegisterRes(false);
        }

        replyToClient(msgReply);
    }
}
