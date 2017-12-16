package com_international.utils;

import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;

import com_international.data.objects.Contact;

import java.util.List;

/**
 * Created by Mor on 10/06/2017.
 */

public interface ContactsUtils extends Utility {

    String[] PROJECTION = new String[]{
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
    };

    Contact getContact(Uri uri, Context context) throws Exception;

    String getContactName(Context context, String phoneNumber);

    String getContactNameHtml(Context context, String phoneNumber);

    List<Contact> getAllContacts(Context context);

    List<String> getAllContactsNumbers(Context context);

    List<String> convertToUids(List<Contact> allContacts);
}
