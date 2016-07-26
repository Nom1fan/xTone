package com.server.actions.v1;

import com.server.actions.ServerAction;
import com.server.annotations.ServerActionAnno;

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

        logger.info("Generating SMS code for [User]:" + _internationePhoneNumber);

        int code = RandUtils.getRand(MIN,MAX);

        //TODO use this after fixing TeleMessageSmsSender to send hebrew
        //ILangStrings strings = StringsFactory.instance().getStrings(data.get(DataKeys.SOURCE_LOCALE).toString());
        //String msg = String.format(strings.your_verification_code(), code);

        String msg = "Your verification code:" + code;

        boolean isOK = smsVerificationAccess.insertSmsVerificationCode(messageInitiaterId, code);

        HashMap replyData = new HashMap();
        if(isOK) {
            smsSender.sendSms(_internationePhoneNumber, msg);
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.GET_SMS_CODE_SUCCESS));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
        }
        else {
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.GET_SMS_CODE_FAILURE));
            replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
        }
    }
}