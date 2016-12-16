package com.actions;

import android.util.Log;

import java.io.IOException;
import java.util.Map;

import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;

/**
 * Created by Mor on 27/04/2016.
 */
public class ClientActionIsRegisteredRes extends ClientAction<Map<DataKeys,Object>> {

    private static final String TAG = ClientActionIsRegisteredRes.class.getSimpleName();

    public ClientActionIsRegisteredRes() {
        super(ClientActionType.IS_REGISTERED_RES);
    }

    @Override
    public EventReport doClientAction(Map<DataKeys,Object> data, int responseCode) throws IOException {

        Log.i(TAG, "Response Code:" + responseCode);

        boolean isRegistered = (boolean) data.get(DataKeys.IS_REGISTERED);
        String phone = (String) data.get(DataKeys.DESTINATION_ID);

        String desc;
        if(isRegistered) {
            desc = "User "+phone+" is registered";
            return new EventReport(EventType.USER_REGISTERED_TRUE, desc, phone);
        }
        else {
            desc = "User "+phone+" is unregistered";
            return new EventReport(EventType.USER_REGISTERED_FALSE, desc, phone);
        }
    }
}
