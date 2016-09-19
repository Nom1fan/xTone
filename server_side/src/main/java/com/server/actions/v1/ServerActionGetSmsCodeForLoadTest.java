package com.server.actions.v1;

import com.server.actions.ServerAction;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 23/04/2016.
 */
@Component("GET_SMS_CODE_FOR_LOAD_TEST")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ServerActionGetSmsCodeForLoadTest extends ServerAction {

    public ServerActionGetSmsCodeForLoadTest() {
        super(ServerActionType.GET_SMS_CODE_FOR_LOAD_TEST);
    }

    @Override
    public void doAction(Map data) {

        logger.info("Generating SMS code for [User]:" + messageInitiaterId);
        int code = 1111;
        boolean isOK = smsVerificationAccess.insertSmsVerificationCode(messageInitiaterId, code);

        if(isOK) {
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.GET_SMS_CODE_SUCCESS));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
        }
        else {
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.GET_SMS_CODE_FAILURE));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
        }
    }
}
