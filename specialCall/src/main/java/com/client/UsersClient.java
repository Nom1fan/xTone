package com.client;

import android.content.Context;

import com.data.objects.Contact;
import com.data.objects.DefaultMediaDataContainer;
import com.data.objects.User;
import com.enums.SpecialMediaType;
import com.google.gson.reflect.TypeToken;
import com.model.response.Response;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by Mor on 24/05/2017.
 */

public interface UsersClient extends Client {

    String URL_GET_REGISTERED_CONTACTS = ROOT_URL + "/v1/GetRegisteredContacts";

    String URL_SYNC_CONTACTS = ROOT_URL + "/v1/SyncContacts";

    List<User> getRegisteredUsers(Context context, List<String> uids);

    void syncContacts(Context context, List<Contact> contacts);
}
