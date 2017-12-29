package com.data.objects;

import com.enums.UserStatus;

/**
 * Created by Mor on 07/03/2017.
 */
public class ContactWrapper extends AbstractDataObject {

    private Contact contact;

    private UserStatus userStatus;

    public ContactWrapper(Contact contact, UserStatus userStatus) {
        this.contact = contact;
        this.userStatus = userStatus;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }
}
