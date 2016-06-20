package actions;

import com.database.SmsVerificationAccess;

import java.sql.SQLException;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.ResponseCodes;
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
        int expectedSmsCode = SmsVerificationAccess.instance(_dao).getSmsVerificationCode(_messageInitiaterId);

        if(smsCode!=SmsVerificationAccess.NO_SMS_CODE && smsCode == expectedSmsCode) {
            try {
                // User device record support was inserted in v1.13
                Double userAppVersion = (Double) data.get(DataKeys.APP_VERSION);
                if (userAppVersion != null && userAppVersion >= ServerConstants.APP_VERSION_1_13) {

                    String deviceModel = (String) data.get(DataKeys.DEVICE_MODEL);
                    String androidVersion = (String) data.get(DataKeys.ANDROID_VERSION);
                    _dao.registerUser(_messageInitiaterId, pushToken, deviceModel, androidVersion);
                } else {
                    _dao.registerUser(_messageInitiaterId, pushToken);
                }
                _replyData.put(DataKeys.IS_REGISTER_SUCCESS, true);
            } catch(SQLException e) {
                _logger.severe("Failed registration for [User]:" + _messageInitiaterId +
                ". [Exception]:" + (e.getMessage()!=null ? e.getMessage() : e));
                _replyData.put(DataKeys.IS_REGISTER_SUCCESS, false);
                _replyData.put(DataKeys.RESPONSE_CODE, ResponseCodes.INTERNAL_SERVER_ERR);
            }
        } else {
            _logger.warning("Rejecting registration for [User]:" + _messageInitiaterId +
                    ". [Expected smsCode]:" + expectedSmsCode + " [Received smsCode]:" + smsCode);
            _replyData.put(DataKeys.IS_REGISTER_SUCCESS, false);
            _replyData.put(DataKeys.RESPONSE_CODE, ResponseCodes.CREDENTIALS_ERR);
        }

        replyToClient(new MessageToClient(ClientActionType.REGISTER_RES, _replyData));

    }
}
