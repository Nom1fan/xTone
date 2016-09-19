package com.server.actions.v1;

import com.server.actions.ServerAction;
import com.server.database.SmsVerificationAccessImpl;
import com.server.lang.ServerConstants;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.ResponseCodes;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 23/04/2016.
 */
@Component("REGISTER")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ServerActionRegister extends ServerAction {

    public ServerActionRegister() {
        super(ServerActionType.REGISTER);
    }

    @Override
    public void doAction(Map data) {

        logger.info(messageInitiaterId + " is attempting to register:" + data);

        int smsCode = (int) data.get(DataKeys.SMS_CODE);
        String pushToken = (String) data.get(DataKeys.PUSH_TOKEN);
        int expectedSmsCode = smsVerificationAccess.getSmsVerificationCode(messageInitiaterId);

        if(smsCode!= SmsVerificationAccessImpl.NO_SMS_CODE && smsCode == expectedSmsCode) {
            try {
                // User device record support was inserted in v1.13
                Double userAppVersion = (Double) data.get(DataKeys.APP_VERSION);
                if (userAppVersion != null && userAppVersion >= ServerConstants.APP_VERSION_1_13) {

                    String deviceModel = (String) data.get(DataKeys.DEVICE_MODEL);
                    String androidVersion = (String) data.get(DataKeys.ANDROID_VERSION);
                    dao.registerUser(messageInitiaterId, pushToken, deviceModel, androidVersion);
                } else {
                    dao.registerUser(messageInitiaterId, pushToken);
                }
                replyData.put(DataKeys.IS_REGISTER_SUCCESS, true);
            } catch(SQLException e) {
                logger.severe("Failed registration for [User]:" + messageInitiaterId +
                ". [Exception]:" + (e.getMessage()!=null ? e.getMessage() : e));
                replyData.put(DataKeys.IS_REGISTER_SUCCESS, false);
                replyData.put(DataKeys.RESPONSE_CODE, ResponseCodes.INTERNAL_SERVER_ERR);
            }
        } else {
            logger.warning("Rejecting registration for [User]:" + messageInitiaterId +
                    ". [Expected smsCode]:" + expectedSmsCode + " [Received smsCode]:" + smsCode);
            replyData.put(DataKeys.IS_REGISTER_SUCCESS, false);
            replyData.put(DataKeys.RESPONSE_CODE, ResponseCodes.CREDENTIALS_ERR);
        }

        replyToClient(new MessageToClient(ClientActionType.REGISTER_RES, replyData));

    }
}
