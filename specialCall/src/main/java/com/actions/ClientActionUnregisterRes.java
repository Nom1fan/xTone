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
public class ClientActionUnregisterRes extends ClientAction<Map<DataKeys,Object>> {

    public ClientActionUnregisterRes() {
        super(ClientActionType.UNREGISTER_RES);
    }

    @Override
    public EventReport doClientAction(Map<DataKeys,Object> data) throws IOException {

        boolean isUnregisterSuccess = (boolean) data.get(DataKeys.IS_UNREGISTER_SUCCESS);

        if(isUnregisterSuccess)
            return new EventReport(EventType.UNREGISTER_SUCCESS, null, null);
        else
            return new EventReport(EventType.UNREGISTER_FAILURE, null, null);
    }
}
