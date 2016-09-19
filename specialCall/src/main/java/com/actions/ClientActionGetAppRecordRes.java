package com.actions;

import java.io.IOException;

import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;

/**
 * Created by Mor on 27/04/2016.
 */
public class ClientActionGetAppRecordRes extends ClientAction<Void> {

    public ClientActionGetAppRecordRes() {
        super(ClientActionType.GET_APP_RECORD_RES);


    }

    @Override
    public EventReport doClientAction(Void data) throws IOException {

        return new EventReport(EventType.APP_RECORD_RECEIVED, null, data);
    }
}
