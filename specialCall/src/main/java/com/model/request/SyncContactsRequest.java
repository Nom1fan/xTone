package com.model.request;

import com.data.objects.Contact;

import java.util.List;
import java.util.Set;

/**
 * Created by Mor on 17/12/2016.
 */
public class SyncContactsRequest extends Request {

    public SyncContactsRequest(Request request) {
        request.copy(this);
    }

    private Set<Contact> contacts;

    public Set<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(Set<Contact> contacts) {
        this.contacts = contacts;
    }
}


