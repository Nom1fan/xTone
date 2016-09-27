package com.actions;

import android.util.Log;

import java.io.IOException;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.ResponseCodes;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 27/04/2016.
 */
public class ClientActionGetAppRecordRes extends ClientAction<Map<DataKeys, Object>> {

    private static final String TAG = ClientActionGetAppRecordRes.class.getSimpleName();

    public ClientActionGetAppRecordRes() {
        super(ClientActionType.GET_APP_RECORD_RES);


    }

    @Override
    public EventReport doClientAction(Map<DataKeys, Object> data) throws IOException {

        if (data.get(DataKeys.MIN_SUPPORTED_VERSION) == null) {
            ResponseCodes responseCode = (ResponseCodes) data.get(DataKeys.RESPONSE_CODE);
            String errMsg = (String) data.get(DataKeys.ERR_MSG);
            log(Log.ERROR, TAG, "Failed to retrieve app record. ResponseCode:[" + responseCode + "] Message:[" + errMsg + "]");
            return new EventReport(EventType.NO_ACTION_REQUIRED, errMsg);
        }

        return new EventReport(EventType.APP_RECORD_RECEIVED, null, data);
    }
}
