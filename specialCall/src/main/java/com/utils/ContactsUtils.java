package com.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
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

}
