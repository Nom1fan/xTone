package com.server.actions.v1;

import com.server.actions.ServerAction;
import com.server.lang.LangStrings;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import utils.RandUtils;

/**
 * Created by Mor on 23/04/2016.
 */
@Component("GET_SMS_CODE")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ServerActionGetSmsCode extends ServerAction {

    private static final int MIN = 1000;
    private static final int MAX = 9999;

    public ServerActionGetSmsCode() {
        super(ServerActionType.GET_SMS_CODE);
    }

    @Override
    public void doAction(Map data) {

        String interPhoneNumber = (String) data.get(DataKeys.INTERNATIONAL_PHONE_NUMBER);

        logger.info("Generating SMS code for [User]:" + interPhoneNumber);

        int code = RandUtils.getRand(MIN,MAX);

        LangStrings strings = stringsFactory.getStrings(data.get(DataKeys.SOURCE_LOCALE).toString());
        String msg = String.format(strings.your_verification_code(), code);

        boolean isOK = smsVerificationAccess.insertSmsVerificationCode(messageInitiaterId, code);

        HashMap<DataKeys, Object> replyData = new HashMap<>();
        if(isOK) {
            smsSender.sendSms(interPhoneNumber, msg);
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.GET_SMS_CODE_SUCCESS));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
        }
        else {
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.GET_SMS_CODE_FAILURE));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
        }
    }
}
