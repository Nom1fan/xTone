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
public class ClientActionUnregisterRes extends ClientAction<Map<DataKeys,Object>> {

    private static final String TAG = ClientActionUnregisterRes.class.getSimpleName();

    public ClientActionUnregisterRes() {
        super(ClientActionType.UNREGISTER_RES);
    }

    @Override
    public EventReport doClientAction(Map<DataKeys,Object> data) throws IOException {

        boolean isUnregisterSuccess = (boolean) data.get(DataKeys.IS_UNREGISTER_SUCCESS);

        if(isUnregisterSuccess)
            return new EventReport(EventType.UNREGISTER_SUCCESS, null);
        else
            return new EventReport(EventType.UNREGISTER_FAILURE, null);
    }
}
