package com.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.data.objects.Contact;

import java.util.ArrayList;
import java.util.List;

import static android.provider.ContactsContract.CommonDataKinds;
import static android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI;
import static android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME;

/**
 * Created by Mor on 18/02/2016.
 */
public class ContactsUtilsImpl implements ContactsUtils {

    @Override
    public Contact getContact(Uri uri, Context context) throws Exception {

        String name = null;
        String number = null;
        if (uri != null) {
            Cursor c = null;
            try {
                c = context.getContentResolver()
                        .query(uri,
                                new String[]{
                                        CommonDataKinds.Phone.NUMBER,
                                        CommonDataKinds.Phone.DISPLAY_NAME,},
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
            throw new Exception("SELECT_CONTACT: _data is null");

        return new Contact(name, number);
    }

    @Override
    public String getContactName(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = "";
        if(cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
        }

        if(!cursor.isClosed()) {
            cursor.close();
        }

        return contactName;
    }

    @Override
    public String getContactNameHtml(Context context, String phoneNumber) {

        return "<b><font color=\"#00FFFF\">" + getContactName(context, phoneNumber) + "</font></b>";
    }

    @Override
    public List<Contact> getAllContacts(Context context) {

        List<Contact> allContacts = new ArrayList<>();
        Cursor people = context.getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI, PROJECTION, null, null, null);

        if (people != null) {
            try {
                final int displayNameIndex = people.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                final int phonesIndex = people.getColumnIndex(CommonDataKinds.Phone.NUMBER);

                String contactName, phoneNumber;

                while (people.moveToNext()) {

                    contactName = people.getString(displayNameIndex);
                    phoneNumber = people.getString(phonesIndex);
                    phoneNumber = PhoneNumberUtils.toValidLocalPhoneNumber(phoneNumber);

                    Contact contact = new Contact(contactName, phoneNumber);
                    if(PhoneNumberUtils.isValidPhoneNumber(phoneNumber) && !allContacts.contains(contact)) {
                        allContacts.add(contact);
                    }
                }
            } finally {
                people.close();
            }
        }
        return allContacts;
    }

    @Override
    public List<String> getAllContactsNumbers(Context context) {
        List<Contact> allContacts = getAllContacts(context);
        List<String> contactNumbers = new ArrayList<>();
        for (Contact contact : allContacts) {
            contactNumbers.add(contact.getPhoneNumber());
        }
        return contactNumbers;
    }

    @Override
    public List<String> convertToUids(List<Contact> allContacts) {
        List<String> uids = new ArrayList<>();
        for (Contact contact : allContacts) {
            uids.add(contact.getPhoneNumber());
        }
        return uids;
    }

}
