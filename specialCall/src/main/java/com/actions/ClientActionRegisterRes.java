package com.actions;

import java.io.IOException;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.ResponseCodes;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;

/**
 * Created by Mor on 27/04/2016.
 */
public class ClientActionRegisterRes extends ClientAction {


    public ClientActionRegisterRes() {
        super(ClientActionType.REGISTER_RES);
    }

    @Override
    public EventReport doClientAction(Map data) throws IOException {

        boolean _isRegisterSuccess = (boolean) data.get(DataKeys.IS_REGISTER_SUCCESS);
        ResponseCodes resCode = (ResponseCodes) data.get(DataKeys.RESPONSE_CODE);

        EventType eventType;
        if(_isRegisterSuccess) {
            eventType = EventType.REGISTER_SUCCESS;
        }
        else {
            eventType = EventType.REGISTER_FAILURE;
        }

        return new EventReport(eventType, null, resCode);
    }
}
