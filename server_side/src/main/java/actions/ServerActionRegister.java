package actions;

import com.database.SmsVerificationAccess;
import com.database.UsersDataAccess;

import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import lang.ServerConstants;

/**
 * Created by Mor on 23/04/2016.
 */
public class ServerActionRegister extends ServerAction {

    public ServerActionRegister() {
        super(ServerActionType.REGISTER);
    }

    @Override
    public void doAction(Map data) {

        _logger.info(_messageInitiaterId + " is attempting to register...");

        int smsCode = (int) data.get(DataKeys.SMS_CODE);
        String pushToken = (String) data.get(DataKeys.PUSH_TOKEN);

        int expectedSmsCode = SmsVerificationAccess.instance(_dal).getSmsVerificationCode(_messageInitiaterId);

        HashMap<DataKeys, Object> replyData = new HashMap();
        if(smsCode!=SmsVerificationAccess.NO_SMS_CODE && smsCode == expectedSmsCode) {

            boolean isRegisteredOK = false;
            // User device record support was inserted in v1.13
            double userAppVersion = (double) data.get(DataKeys.APP_VERSION);
            if(userAppVersion >= ServerConstants.APP_VERSION_1_13) {

                String deviceModel = (String) data.get(DataKeys.DEVICE_MODEL);
                String androidVersion = (String) data.get(DataKeys.ANDROID_VERSION);
                isRegisteredOK = UsersDataAccess.instance(_dal).registerUser(_messageInitiaterId, pushToken, deviceModel, androidVersion);
            }
            else {
                isRegisteredOK = UsersDataAccess.instance(_dal).registerUser(_messageInitiaterId, pushToken);
            }
            replyData.put(DataKeys.IS_REGISTER_SUCCESS, isRegisteredOK);
        } else {
            _logger.warning("Rejecting registration for [User]:" + _messageInitiaterId +
                    ". [Expected smsCode]:" + expectedSmsCode + " [Received smsCode]:" + smsCode);
            replyData.put(DataKeys.IS_REGISTER_SUCCESS, false);
        }

        replyToClient(new MessageToClient(ClientActionType.REGISTER_RES, replyData));

    }
}
