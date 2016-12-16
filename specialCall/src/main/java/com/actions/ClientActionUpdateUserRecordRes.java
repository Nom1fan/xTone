package com.actions;

import java.io.IOException;
import java.util.Map;

import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;

/**
 * Created by Mor on 27/04/2016.
 */
public class ClientActionUpdateUserRecordRes extends ClientAction<Map<DataKeys,Object>> {


    public ClientActionUpdateUserRecordRes() {
        super(ClientActionType.UPDATE_RES);
    }

    @Override
    public EventReport doClientAction(Map<DataKeys,Object> data, int responseCode) throws IOException {

        boolean _isRegisterSuccess = (boolean) data.get(DataKeys.IS_UPDATE_SUCCESS);

        EventType eventType;
        if(_isRegisterSuccess) {
            eventType = EventType.UPDATE_USER_RECORD_SUCCESS;
        }
        else {
            eventType = EventType.UPDATE_USER_RECORD_FAILURE;
        }

        return new EventReport(eventType, null, null);
    }
}
