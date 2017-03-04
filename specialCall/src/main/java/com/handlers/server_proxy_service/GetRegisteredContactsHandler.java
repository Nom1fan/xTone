package com.handlers.server_proxy_service;

import android.os.Build;
import android.util.Log;

import com.client.ConnectionToServer;
import com.data.objects.Constants;
import com.data.objects.User;
import com.event.EventReport;
import com.event.EventType;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.handlers.background_broadcast_receiver.EventLoadingTimeoutHandler;
import com.model.request.GetRegisteredContactsRequest;
import com.model.request.IsRegisteredRequest;
import com.model.request.Request;
import com.model.response.Response;
import com.utils.BroadcastUtils;
import com.utils.RequestUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;
import static com.services.ServerProxyService.CONTACTS_UIDS;
import static com.services.ServerProxyService.DESTINATION_ID;

/**
 * Created by Mor on 04/03/2017.
 */
public class GetRegisteredContactsHandler implements ActionHandler {
    private static final String URL_GET_REGISTERED_CONTACTS = ROOT_URL + "/v1/GetRegisteredContacts";
    private static final String TAG = GetRegisteredContactsHandler.class.getSimpleName();
    private static final Type responseType = new TypeToken<Response<List<User>>>() {
    }.getType();

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        ArrayList<String> contactsUids = actionBundle.getIntent().getStringArrayListExtra(CONTACTS_UIDS);
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();
        connectionToServer.setResponseType(responseType);
        GetRegisteredContactsRequest getRegisteredContactsRequest = new GetRegisteredContactsRequest(actionBundle.getRequest());
        getRegisteredContactsRequest.setContactsUids(contactsUids);
        getRegisteredContactsRequest.setLocale(Locale.getDefault().getLanguage());
        log(Log.INFO, TAG, "Initiating GetRegisteredContacts sequence...");
        int responseCode = connectionToServer.sendRequest(URL_GET_REGISTERED_CONTACTS, getRegisteredContactsRequest);

        if(responseCode == HttpStatus.SC_OK) {
            Response<List<User>> response = connectionToServer.readResponse();
            List<User> registeredContacts = response.getResult();
            log(Log.DEBUG, TAG, "Retrieved registered contacts:" + convertUsersToUidsString(registeredContacts));
            BroadcastUtils.sendEventReportBroadcast(actionBundle.getCtx(), TAG, new EventReport(EventType.GET_REGISTERED_CONTACTS_SUCCESS, registeredContacts));
        }
        else {
            log(Log.ERROR, TAG, "Failed to fetch registered contacts. Response code was:" + responseCode);
            EventLoadingTimeoutHandler timeoutHandler = new EventLoadingTimeoutHandler();
            timeoutHandler.handle(actionBundle.getCtx());
        }
    }

    private String convertUsersToUidsString(List<User> users) {
        StringBuilder builder = new StringBuilder("[");
        for (User user : users) {
            builder.append(user.getUid())
                    .append(",");
        }
        builder.replace(builder.length() - 1, builder.length(), "]");
        return builder.toString();
    }
}
