package com.services;

import android.content.Context;

import com.client.ClientFactory;
import com.client.UsersClient;
import com.logger.Logger;
import com.logger.LoggerFactory;
import com.logic.Logic;
import com.utils.ContactsUtils;
import com.utils.UtilityFactory;

/**
 * Created by Mor on 12/08/2017.
 */

public class SyncContactsLogic implements Logic {

    private static final String TAG = SyncContactsLogic.class.getSimpleName();

    private Logger logger = LoggerFactory.getLogger();

    private UsersClient usersClient;

    private ContactsUtils contactsUtils;

    private Context context;

    public SyncContactsLogic() {
    }

    public SyncContactsLogic(Context context) {
        this.context = context;
        this.usersClient = ClientFactory.getInstance().getClient(UsersClient.class);
        this.contactsUtils = UtilityFactory.instance().getUtility(ContactsUtils.class);
    }

    @Override
    public void executeLogic() {
        logger.info(TAG, "Synchronizing contacts...");
        usersClient.syncContacts(context, contactsUtils.getAllContacts(context));
    }

    public UsersClient getUsersClient() {
        return usersClient;
    }

    public void setUsersClient(UsersClient usersClient) {
        this.usersClient = usersClient;
    }

    public ContactsUtils getContactsUtils() {
        return contactsUtils;
    }

    public void setContactsUtils(ContactsUtils contactsUtils) {
        this.contactsUtils = contactsUtils;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
