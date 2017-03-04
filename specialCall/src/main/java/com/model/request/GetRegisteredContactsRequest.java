package com.model.request;

import java.util.List;

/**
 * Created by Mor on 1/18/2017.
 */
public class GetRegisteredContactsRequest extends Request {

    private List<String> contactsUids;

    public GetRegisteredContactsRequest(Request request) {
        request.copy(this);
    }

    public List<String> getContactsUids() {
        return contactsUids;
    }

    public void setContactsUids(List<String> contactsUids) {
        this.contactsUids = contactsUids;
    }
}
