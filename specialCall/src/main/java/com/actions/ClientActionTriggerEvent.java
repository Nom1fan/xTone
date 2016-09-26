package com.actions;

import java.io.IOException;

import EventObjects.EventReport;
import MessagesToClient.ClientActionType;

/**
 * Created by Mor on 27/04/2016.
 */
public class ClientActionTriggerEvent extends ClientAction<EventReport> {

    public ClientActionTriggerEvent() {
        super(ClientActionType.TRIGGER_EVENT);
    }

    @Override
    public EventReport doClientAction(EventReport eventReport) throws IOException {
        return eventReport;
    }
}
