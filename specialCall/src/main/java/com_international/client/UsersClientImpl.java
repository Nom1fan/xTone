package com_international.client;

import android.content.Context;

import com_international.data.objects.User;
import com_international.logger.Logger;
import com_international.logger.LoggerFactory;
import com_international.model.request.GetRegisteredContactsRequest;
import com_international.model.request.Request;
import com_international.model.response.Response;
import com_international.utils.RequestUtils;

import org.apache.http.HttpStatus;

import java.util.List;
import java.util.Locale;

/**
 * Created by Mor on 01/07/2017.
 */

public class UsersClientImpl implements UsersClient {

    private static final String TAG = UsersClientImpl.class.getSimpleName();

    private Logger logger = LoggerFactory.getLogger();

    @Override
    public List<User> getRegisteredUsers(Context context, List<String> uids) {
        Response<List<User>> registeredContacts = null;
        ConnectionToServerImpl connectionToServer = new ConnectionToServerImpl();
        try {
            connectionToServer.setResponseType(responseType);
            Request defaultRequest = RequestUtils.getDefaultRequest(context);
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
}
