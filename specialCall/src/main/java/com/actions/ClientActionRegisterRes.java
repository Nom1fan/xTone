package com.actions;

import com.event.EventReport;
import com.event.EventType;
import com.model.response.ClientActionType;
import com.model.response.Response;

import java.io.IOException;

import cz.msebera.android.httpclient.HttpStatus;

/**
 * Created by Mor on 27/04/2016.
 */
public class ClientActionRegisterRes extends ClientAction<Response> {


    public ClientActionRegisterRes() {
        super(ClientActionType.REGISTER_RES);
    }

    @Override
    public EventReport doClientAction(Response response) throws IOException {

        EventType eventType;
        String errMsg = null;
        int responseCode = response.getResponseCode();
        if(responseCode == HttpStatus.SC_OK) {
            eventType = EventType.REGISTER_SUCCESS;
        }
        else {
            eventType = EventType.REGISTER_FAILURE;
            errMsg = "Registration failed. [Response code]:" + responseCode + " [Response message]:" + response.getMessage();
        }

        return new EventReport(eventType, errMsg, responseCode);
    }
}
