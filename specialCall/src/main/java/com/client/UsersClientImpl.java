package com.client;

import android.content.Context;

import com.data.objects.Contact;
import com.data.objects.User;
import com.google.gson.reflect.TypeToken;
import com.logger.Logger;
import com.logger.LoggerFactory;
import com.model.request.GetRegisteredContactsRequest;
import com.model.request.Request;
import com.model.request.SyncContactsRequest;
import com.model.response.Response;
import com.utils.RequestUtils;
import com.utils.UtilityFactory;

import org.apache.http.HttpStatus;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Created by Mor on 01/07/2017.
 */

public class UsersClientImpl implements UsersClient {

    private static final String TAG = UsersClientImpl.class.getSimpleName();

    private final RequestUtils requestUtils = UtilityFactory.instance().getUtility(RequestUtils.class);

    private Logger logger = LoggerFactory.getLogger();

    @Override
    public List<User> getRegisteredUsers(Context context, List<String> uids) {
        Type responseType = new TypeToken<Response<List<User>>>() {}.getType();
        Response<List<User>> registeredContacts = null;
        ConnectionToServerImpl connectionToServer = new ConnectionToServerImpl();
        try {
            connectionToServer.setResponseType(responseType);
            Request defaultRequest = requestUtils.getDefaultRequest(context);
            GetRegisteredContactsRequest getRegisteredContactsRequest = new GetRegisteredContactsRequest(defaultRequest);
            getRegisteredContactsRequest.setContactsUids(uids);
            getRegisteredContactsRequest.setLocale(Locale.getDefault().getLanguage());
            logger.info(TAG, "Initiating GetRegisteredContacts sequence...");
            int responseCode = connectionToServer.sendRequest(URL_GET_REGISTERED_CONTACTS, getRegisteredContactsRequest);
            logger.info(TAG, "Response code received:[" + responseCode + "]");
            if(responseCode == HttpStatus.SC_OK) {
                registeredContacts = connectionToServer.readResponse();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return registeredContacts == null ? null : registeredContacts.getResult();
    }

    @Override
    public void syncContacts(Context context, List<Contact> contacts) {
        ConnectionToServerImpl connectionToServer = new ConnectionToServerImpl();
        Request defaultRequest = requestUtils.getDefaultRequest(context);
        SyncContactsRequest syncContactsRequest = new SyncContactsRequest(defaultRequest);
        syncContactsRequest.setContacts(new HashSet<>(contacts));
        try {
            logger.info(TAG, "Initiating syncContacts sequence...");
            int responseCode = connectionToServer.sendRequest(URL_SYNC_CONTACTS, syncContactsRequest);
            if(responseCode != HttpStatus.SC_OK) {
                logger.error(TAG, "Response code received:[" + responseCode + "]");

            } else {
                logger.info(TAG, "Response code received:[" + responseCode + "]");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
