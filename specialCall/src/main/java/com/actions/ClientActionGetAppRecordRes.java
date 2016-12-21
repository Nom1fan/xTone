package com.actions;

import android.util.Log;

import com.event.EventReport;
import com.event.EventType;
import com.model.response.ClientActionType;
import com.model.response.AppMetaDTO;
import com.model.response.Response;

import java.io.IOException;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 27/04/2016.
 */
public class ClientActionGetAppRecordRes extends ClientAction<Response<AppMetaDTO>> {

    private static final String TAG = ClientActionGetAppRecordRes.class.getSimpleName();

    public ClientActionGetAppRecordRes() {
        super(ClientActionType.GET_APP_RECORD_RES);
    }

    @Override
    public EventReport doClientAction(Response<AppMetaDTO> response) throws IOException {

        AppMetaDTO result = response.getResult();
        if (result.getLastSupportedAppVersion() == null) {
            String errMsg = response.getMessage();
            int responseCode = response.getResponseCode();
            log(Log.ERROR, TAG, "Failed to retrieve app record. ResponseCode:[" + responseCode + "] Message:[" + errMsg + "]");
            return new EventReport(EventType.NO_ACTION_REQUIRED, errMsg);
        }

        return new EventReport(EventType.APP_RECORD_RECEIVED, null, result.getLastSupportedAppVersion());
    }
}
