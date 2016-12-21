package com.actions;

import com.data.objects.DataKeys;
import com.event.EventReport;
import com.event.EventType;
import com.model.response.ClientActionType;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Mor on 27/04/2016.
 */
public class ClientActionUpdateUserRecordRes extends ClientAction<Map<DataKeys,Object>> {


    public ClientActionUpdateUserRecordRes() {
        super(ClientActionType.UPDATE_RES);
    }

    @Override
    public EventReport doClientAction(Map<DataKeys,Object> data) throws IOException {

        boolean _isRegisterSuccess = (boolean) data.get(DataKeys.IS_UPDATE_SUCCESS);

        EventType eventType;
        if(_isRegisterSuccess) {
            eventType = EventType.UPDATE_USER_RECORD_SUCCESS;
        }
        else {
            eventType = EventType.UPDATE_USER_RECORD_FAILURE;
        }

        return new EventReport(eventType, null, null);
    }
}
