package com.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.ContactsContract;

import com.data_objects.Contact;

import java.util.ArrayList;
import java.util.List;

import utils.PhoneNumberUtils;

/**
 * Created by Mor on 18/02/2016.
 */
public abstract class ContactsUtils {

    private static final String[] PROJECTION = new String[]{
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
    };

    public static Contact getContact(Uri uri, Context context) throws Exception {

        String name = null;
        String number = null;
        if (uri != null) {
            Cursor c = null;
            try {
                c = context.getContentResolver()
                        .query(uri,
                                new String[]{
                                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,},
                                null, null, null);

                if (c != null && c.moveToFirst()) {
                    number = c.getString(0);
                    name = c.getString(1);

                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        } else
            throw new Exception("SELECT_CONTACT: data is null");

        return new Contact(name, number);
    }

    //TODO Is there a way to do this without reflection?
    public static String getContactName(final Context context, final String phoneNumber) {
        Uri uri;
        String[] projection;
        Uri mBaseUri = Contacts.Phones.CONTENT_FILTER_URL;
        projection = new String[]{android.provider.Contacts.People.NAME};
        try {
            Class<?> c = Class.forName("android.provider.ContactsContract$PhoneLookup");
            mBaseUri = (Uri) c.getField("CONTENT_FILTER_URI").get(mBaseUri);
            projection = new String[]{"display_name"};
        } catch (Exception e) {
        } // Why are we obsorbing the exception?

        uri = Uri.withAppendedPath(mBaseUri, Uri.encode(phoneNumber));
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

        String contactName = "";

        if (cursor.moveToFirst()) {
            contactName = cursor.getString(0);
        }

        cursor.close();
        cursor = null;

        if(contactName == null || contactName.equals(""))
            return phoneNumber;

        return contactName;
    }

    public static List<Contact> getAllContacts(Context context) {

        List<Contact> allContacts = new ArrayList<>();
        Cursor people = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION, null, null, null);

        if (people != null) {
            try {
                final int displayNameIndex = people.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                final int phonesIndex = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                String contactName, phoneNumber;

                while (people.moveToNext()) {

                    contactName = people.getString(displayNameIndex);
                    phoneNumber = people.getString(phonesIndex);
                    phoneNumber = PhoneNumberUtils.toValidLocalPhoneNumber(phoneNumber);

                    if(PhoneNumberUtils.isValidPhoneNumber(phoneNumber))
                        allContacts.add(new Contact(contactName, phoneNumber));
                }
            } finally {
                people.close();
            }
        }
        return allContacts;
    }
}
