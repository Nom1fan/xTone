package com.handlers.logic_server_proxy_service;

import android.util.Log;

import com.client.ConnectionToServer;
import com.data.objects.UserStatus;
import com.event.EventReport;
import com.event.EventType;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.model.request.IsRegisteredRequest;
import com.model.response.Response;
import com.model.response.UserDTO;
import com.utils.BroadcastUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;
import static com.services.ServerProxyService.DESTINATION_ID;

/**
 * Created by Mor on 20/12/2016.
 */
public class IsRegisteredActionHandler implements ActionHandler {

    protected static final String URL_ISREGISTERED = ROOT_URL + "/v1/IsRegistered";
    private static final String TAG = IsRegisteredActionHandler.class.getSimpleName();
    private static final Type responseType = new TypeToken<Response<UserDTO>>() {
    }.getType();

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        String destinationId = actionBundle.getIntent().getStringExtra(DESTINATION_ID);
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();
        connectionToServer.setResponseType(responseType);
        IsRegisteredRequest isRegisteredRequest = new IsRegisteredRequest(actionBundle.getRequest());
        isRegisteredRequest.setDestinationId(destinationId);
        isRegisteredRequest.setSourceLocale(Locale.getDefault().getLanguage());
        log(Log.INFO, TAG, "Initiating IsRegistered sequence...");
        int responseCode = connectionToServer.sendRequest(URL_ISREGISTERED, isRegisteredRequest);

        EventReport eventReport;
        String desc;
        if (responseCode == HttpStatus.SC_OK) {
            Response<UserDTO> response = connectionToServer.readResponse();
            UserDTO userDTO = response.getResult();
            String uid = userDTO.getUid();
            if (userDTO.getUserStatus().equals(UserStatus.REGISTERED)) {
                desc = "User " + uid + " is registered";
                eventReport = new EventReport(EventType.USER_REGISTERED_TRUE, desc, uid);
            }
            else {
                desc = "User " + uid + "is " + userDTO.getUserStatus().toString().toLowerCase();
                eventReport = new EventReport(EventType.USER_REGISTERED_FALSE, desc, uid);
            }
            BroadcastUtils.sendEventReportBroadcast(actionBundle.getCtx(), TAG, eventReport);
        }
        else {
            log(Log.ERROR, TAG, "IsRegistered request failed. [Response code]:" + responseCode);
        }
    }
}
