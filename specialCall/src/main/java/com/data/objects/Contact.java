package com.data.objects;

/**
 * Created by Mor on 18/02/2016.
 */
public class Contact extends AbstractDataObject{

    private String contactName;
    private String contactUid;

    public Contact(String contactName, String contactUid) {
        this.contactName = contactName;
        this.contactUid = contactUid;
    }

    public String getContactName() {
        return contactName;
    }

    public String getPhoneNumber() {
        return contactUid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Contact contact = (Contact) o;

        return contactUid.equals(contact.contactUid);

    }

    @Override
    public int hashCode() {
        return contactUid.hashCode();
    }

    @Override
    public String toString() {
        return "Contact{" +
                "contactName='" + contactName + '\'' +
                ", phoneNumber='" + contactUid + '\'' +
                '}';
    }
}
