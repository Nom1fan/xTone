package com.actions;

import android.util.Log;

import java.io.IOException;

import EventObjects.EventReport;
import MessagesToClient.ClientActionType;

/**
 * Created by Mor on 27/04/2016.
 */
public class ClientActionTriggerEvent extends ClientAction<EventReport> {

    private static final String TAG = ClientActionTriggerEvent.class.getSimpleName();

    public ClientActionTriggerEvent() {
        super(ClientActionType.TRIGGER_EVENT);
    }

    @Override
    public EventReport doClientAction(EventReport eventReport, int responseCode) throws IOException {

        Log.i(TAG, "Response code:" + responseCode);

        return eventReport;
    }
}
