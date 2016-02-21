package com.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.ContactsContract;

import com.data_objects.Contact;

/**
 * Created by Mor on 18/02/2016.
 */
public abstract class ContactsUtils {

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
            }
            finally {
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
        projection = new String[] { android.provider.Contacts.People.NAME };
        try {
            Class<?> c = Class.forName("android.provider.ContactsContract$PhoneLookup");
            mBaseUri = (Uri) c.getField("CONTENT_FILTER_URI").get(mBaseUri);
            projection = new String[] { "display_name" };
        }
        catch (Exception e) { } // Why are we obsorbing the exception?

        uri = Uri.withAppendedPath(mBaseUri, Uri.encode(phoneNumber));
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

        String contactName = "";

        if (cursor.moveToFirst())
        {
            contactName = cursor.getString(0);
        }

        cursor.close();
        cursor = null;

        return contactName;
    }
}
