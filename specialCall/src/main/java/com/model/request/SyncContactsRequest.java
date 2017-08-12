package com.model.request;

import com.data.objects.Contact;

import java.util.List;

/**
 * Created by Mor on 17/12/2016.
 */
public class SyncContactsRequest extends Request {

    public SyncContactsRequest(Request request) {
        request.copy(this);
    }

    List<Contact> contacts;
}


