package com.handlers.server_proxy_service;

import android.util.Log;

import com.app.AppStateManager;
import com.client.ClientFactory;
import com.client.UsersClient;
import com.data.objects.Contact;
import com.data.objects.ContactWrapper;
import com.data.objects.User;
import com.enums.UserStatus;
import com.event.EventReport;
import com.event.EventType;
import com.google.gson.reflect.TypeToken;
import com.handlers.ActionHandler;
import com.handlers.background_broadcast_receiver.EventLoadingTimeoutHandler;
import com.model.response.Response;
import com.utils.BroadcastUtils;
import com.utils.ContactsUtils;
import com.utils.UtilityFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 04/03/2017.
 */
public class GetRegisteredContactsHandler implements ActionHandler {

    private static final String TAG = GetRegisteredContactsHandler.class.getSimpleName();

    private final ContactsUtils contactsUtils = UtilityFactory.instance().getUtility(ContactsUtils.class);

    private UsersClient usersClient = ClientFactory.getInstance().getClient(UsersClient.class);

    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        List<Contact> allContacts = contactsUtils.getAllContacts(actionBundle.getCtx());
        List<String> contactsUids = contactsUtils.convertToUids(allContacts);
        List<User> registeredUsers = usersClient.getRegisteredUsers(actionBundle.getCtx(), contactsUids);

        if(registeredUsers != null) {
            log(Log.DEBUG, TAG, "Retrieved registered contacts:" + convertUsersToUidsString(registeredUsers));

            List<ContactWrapper> registeredContacts = wrapAndSort(allContacts, registeredUsers);
            AppStateManager.setAppState(actionBundle.getCtx(), TAG, AppStateManager.STATE_IDLE);
            BroadcastUtils.sendEventReportBroadcast(actionBundle.getCtx(), TAG, new EventReport(EventType.GET_REGISTERED_CONTACTS_SUCCESS, registeredContacts));
        }
        else {
            EventLoadingTimeoutHandler timeoutHandler = new EventLoadingTimeoutHandler();
            timeoutHandler.handle(actionBundle.getCtx());
        }
    }

    private List<ContactWrapper> wrapAndSort(List<Contact> allContacts, List<User> registeredUsers) {
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
