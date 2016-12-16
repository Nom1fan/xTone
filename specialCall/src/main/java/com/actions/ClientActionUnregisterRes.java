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
public class ClientActionUnregisterRes extends ClientAction<Map<DataKeys,Object>> {

    private static final String TAG = ClientActionUnregisterRes.class.getSimpleName();

    public ClientActionUnregisterRes() {
        super(ClientActionType.UNREGISTER_RES);
    }

    @Override
    public EventReport doClientAction(Map<DataKeys,Object> data, int responseCode) throws IOException {

        Log.i(TAG, "Response code:" + responseCode);

        boolean isUnregisterSuccess = (boolean) data.get(DataKeys.IS_UNREGISTER_SUCCESS);

        if(isUnregisterSuccess)
            return new EventReport(EventType.UNREGISTER_SUCCESS, null, responseCode);
        else
            return new EventReport(EventType.UNREGISTER_FAILURE, null, responseCode);
    }
}
