package com.handlers.server_proxy_service;

import android.content.Context;
import android.util.Log;

import com.app.AppStateManager;
import com.client.ConnectionToServer;
import com.data.objects.Contact;
import com.data.objects.ContactWrapper;
import com.data.objects.SnackbarData;
import com.data.objects.User;
import com.enums.UserStatus;
import com.event.EventReport;
import com.event.EventType;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.handlers.background_broadcast_receiver.EventLoadingTimeoutHandler;
import com.model.request.GetRegisteredContactsRequest;
import com.model.response.Response;
import com.utils.BroadcastUtils;
import com.utils.ContactsUtils;
import com.utils.UI_Utils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;
import static com.data.objects.SnackbarData.*;

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
        List<Contact> allContacts = ContactsUtils.getAllContacts(actionBundle.getCtx());
        List<String> contactsUids = convertToUids(allContacts);
        ConnectionToServer connectionToServer = actionBundle.getConnectionToServer();
        connectionToServer.setResponseType(responseType);
        GetRegisteredContactsRequest getRegisteredContactsRequest = new GetRegisteredContactsRequest(actionBundle.getRequest());
        getRegisteredContactsRequest.setContactsUids(contactsUids);
        getRegisteredContactsRequest.setLocale(Locale.getDefault().getLanguage());
        log(Log.INFO, TAG, "Initiating GetRegisteredContacts sequence...");
        int responseCode = connectionToServer.sendRequest(URL_GET_REGISTERED_CONTACTS, getRegisteredContactsRequest);

        if(responseCode == HttpStatus.SC_OK) {
            Response<List<User>> response = connectionToServer.readResponse();
            List<User> registeredUsers = response.getResult();
            log(Log.DEBUG, TAG, "Retrieved registered contacts:" + convertUsersToUidsString(registeredUsers));

            List<ContactWrapper> registeredContacts = wrapAndSort(actionBundle.getCtx(), allContacts, registeredUsers);
            AppStateManager.setAppState(actionBundle.getCtx(), TAG, AppStateManager.STATE_IDLE);
            BroadcastUtils.sendEventReportBroadcast(actionBundle.getCtx(), TAG, new EventReport(EventType.GET_REGISTERED_CONTACTS_SUCCESS, registeredContacts));
        }
        else {
            log(Log.ERROR, TAG, "Failed to fetch registered contacts. Response code was:" + responseCode);
            EventLoadingTimeoutHandler timeoutHandler = new EventLoadingTimeoutHandler();
            timeoutHandler.handle(actionBundle.getCtx());
        }
    }

    private List<String> convertToUids(List<Contact> allContacts) {
        List<String> uids = new ArrayList<>();
        for (Contact contact : allContacts) {
            uids.add(contact.getPhoneNumber());
        }
        return uids;
    }

    private List<ContactWrapper> wrapAndSort(Context ctx, List<Contact> allContacts, List<User> registeredUsers) {
        List<ContactWrapper> contactWrappers = new ArrayList<>();
        List<Contact> registeredContacts = convertToContacts(registeredUsers);
        addRegisteredContacts(contactWrappers, registeredContacts, allContacts);
        allContacts.removeAll(registeredContacts);
        addUnregisteredContacts(contactWrappers, allContacts);
        return contactWrappers;
    }

    private void addUnregisteredContacts(List<ContactWrapper> contactWrappers, List<Contact> allContacts) {
        for (Contact contact : allContacts) {
            contactWrappers.add(new ContactWrapper(contact, UserStatus.UNREGISTERED));
        }
    }

    private void addRegisteredContacts(List<ContactWrapper> contactWrappers, List<Contact> registeredContacts, List<Contact> allContacts) {
        for (Contact contact : allContacts) {
            if(registeredContacts.contains(contact)) {
                contactWrappers.add(new ContactWrapper(contact, UserStatus.REGISTERED));
            }
        }
    }

    private List<Contact> convertToContacts(List<User> registeredUsers) {
        List<Contact> registeredContacts = new ArrayList<>();
        for (User registeredUser : registeredUsers) {
            registeredContacts.add(new Contact("", registeredUser.getUid()));
        }
        return registeredContacts;
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
