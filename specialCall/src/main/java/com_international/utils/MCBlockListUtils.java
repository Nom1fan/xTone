package com_international.utils;

import android.content.Context;
import android.util.Log;

import com_international.data.objects.Contact;
import com_international.enums.PermissionBlockListLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by rony on 27/02/2016.
 */
public abstract class MCBlockListUtils {

    private static final String TAG = MCBlockListUtils.class.getSimpleName();

    private static final ContactsUtils contactsUtils = UtilityFactory.instance().getUtility(ContactsUtils.class);

    public static boolean IsMCBlocked(String incomingNumber, Context context) {
        log(Log.INFO,TAG, "check if number blocked: " + incomingNumber);
        //MC Permissions: ALL , Only contacts , Specific Black List Contacts
        PermissionBlockListLevel permissionLevel = SettingsUtils.getWhoCanMCMe(context);

        if (permissionLevel.equals(PermissionBlockListLevel.EMPTY)) {
            SettingsUtils.setWhoCanMCMe(context, PermissionBlockListLevel.ALL_VALID);
        }
        else {
            switch (permissionLevel) {

                case ALL_VALID:
                    Set<String> blockedSet1 = SharedPrefUtils.getStringSet(context, SharedPrefUtils.SETTINGS, SharedPrefUtils.BLOCK_LIST);
                    if (!blockedSet1.isEmpty()) {
                        incomingNumber = PhoneNumberUtils.toValidLocalPhoneNumber(incomingNumber);

                        if (blockedSet1.contains(incomingNumber)) {
                            log(Log.INFO,TAG, "ALL_VALID NUMBER MC BLOCKED: " + incomingNumber);
                            return true;
                        }
                    }
                    else {
                        log(Log.WARN,TAG, "ALL_VALID BlackList empty allowing phone number: " + incomingNumber);
                        return false;
                    }

                    return false;

                case CONTACTS_ONLY:
                    // GET ALL CONTACTS
                    List<Contact> contactsList = contactsUtils.getAllContacts(context);
                    List<String> contactPhonenumbers = new ArrayList<>();

                    for (int i=0; i<contactsList.size(); i++) {
                        contactPhonenumbers.add(contactsList.get(i).getPhoneNumber());
                    }

                    Set<String> blockedSet2 = SharedPrefUtils.getStringSet(context, SharedPrefUtils.SETTINGS, SharedPrefUtils.BLOCK_LIST);
                    incomingNumber = PhoneNumberUtils.toValidLocalPhoneNumber(incomingNumber);

                    return !(contactPhonenumbers.contains(incomingNumber) && !blockedSet2.contains(incomingNumber));

                case NO_ONE:
                    return true;

                case BLACK_LIST_SPECIFIC:
                    Set<String> blockedSet = SharedPrefUtils.getStringSet(context, SharedPrefUtils.SETTINGS, SharedPrefUtils.BLOCK_LIST);
                    if (!blockedSet.isEmpty()) {
                        incomingNumber = PhoneNumberUtils.toValidLocalPhoneNumber(incomingNumber);

                        if (blockedSet.contains(incomingNumber)) {
                            log(Log.INFO,TAG, "BLACK_LIST_SPECIFIC NUMBER MC BLOCKED: " + incomingNumber);
                            return true;
                        }
                    }
                    else {
                        log(Log.WARN,TAG, "BLACK_LIST_SPECIFIC BlackList empty allowing phone number: " + incomingNumber);
                        return false;
                    }
            }
        }
        return false;
    }

    public static Set<String> getBlockListFromShared(Context context){
       return SharedPrefUtils.getStringSet(context, SharedPrefUtils.SETTINGS, SharedPrefUtils.BLOCK_LIST);
    }

    public static void setBlockListFromShared(Context context,Set<String> blockList){
        SharedPrefUtils.remove(context, SharedPrefUtils.SETTINGS, SharedPrefUtils.BLOCK_LIST);
        SharedPrefUtils.setStringSet(context, SharedPrefUtils.SETTINGS, SharedPrefUtils.BLOCK_LIST, blockList);
        SettingsUtils.setWhoCanMCMe(context, PermissionBlockListLevel.BLACK_LIST_SPECIFIC);
    }

}
