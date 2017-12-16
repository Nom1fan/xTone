package com_international.client;

import android.content.Context;

import com_international.data.objects.User;

import com.google.gson.reflect.TypeToken;
import com_international.model.response.Response;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by Mor on 24/05/2017.
 */

public interface UsersClient extends Client {

    String URL_GET_REGISTERED_CONTACTS = ROOT_URL + "/v1/GetRegisteredContacts";
    Type responseType = new TypeToken<Response<List<User>>>() {
    }.getType();

    List<User> getRegisteredUsers(Context context, List<String> uids);
}
