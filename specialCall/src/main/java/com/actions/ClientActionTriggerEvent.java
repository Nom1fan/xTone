package com.actions;

import java.io.IOException;
import java.util.Map;

import DataObjects.DataKeys;
import EventObjects.EventReport;
import MessagesToClient.ClientActionType;

/**
 * Created by Mor on 27/04/2016.
 */
public class ClientActionTriggerEvent extends ClientAction<Map<DataKeys,Object>> {

    public ClientActionTriggerEvent() {
        super(ClientActionType.TRIGGER_EVENT);
    }

    @Override
    public EventReport doClientAction(Map<DataKeys,Object> data) throws IOException {

        return (EventReport) data.get(DataKeys.EVENT_REPORT);
    }
}
