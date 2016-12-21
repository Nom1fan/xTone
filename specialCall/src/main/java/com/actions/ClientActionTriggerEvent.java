package com.actions;

import com.event.EventReport;
import com.model.response.ClientActionType;

import java.io.IOException;

/**
 * Created by Mor on 27/04/2016.
 */
public class ClientActionTriggerEvent extends ClientAction<EventReport> {

    private static final String TAG = ClientActionTriggerEvent.class.getSimpleName();

    public ClientActionTriggerEvent() {
        super(ClientActionType.TRIGGER_EVENT);
    }

    @Override
    public EventReport doClientAction(EventReport eventReport) throws IOException {

        return eventReport;
    }
}
