package com.actions;

import java.io.IOException;
import java.util.Map;

import DataObjects.DataKeys;
import EventObjects.EventReport;
import MessagesToClient.ClientActionType;

/**
 * Created by Mor on 27/04/2016.
 */
public class ClientActionTriggerEvent extends ClientAction {

    public ClientActionTriggerEvent() {
        super(ClientActionType.TRIGGER_EVENT);
    }

    @Override
    public EventReport doClientAction(Map data) throws IOException {

        EventReport eventReport = (EventReport) data.get(DataKeys.EVENT_REPORT);

        return eventReport;
    }
}
