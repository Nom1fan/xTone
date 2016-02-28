package com.utils;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import com.data_objects.PermissionBlockListLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by rony on 27/02/2016.
 */
public abstract class MCBlockListUtils {

    private static final String TAG = MCBlockListUtils.class.getSimpleName();

    public static boolean checkIfNumberIsMCBlocked(String incomingNumber,Context context) {
        Log.i(TAG, "check if number blocked: " + incomingNumber);
        //MC Permissions: ALL , Only contacts , Specific Black List Contacts
        String permissionLevel = SharedPrefUtils.getString(context, SharedPrefUtils.RADIO_BUTTON_SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME);

        if (permissionLevel.isEmpty())
        {
            SharedPrefUtils.setString(context, SharedPrefUtils.RADIO_BUTTON_SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME, PermissionBlockListLevel.ALL_VALID);
        }
        else
        {
            switch (permissionLevel) {

                case PermissionBlockListLevel.ALL_VALID:
                    return false;

                case PermissionBlockListLevel.CONTACTS_ONLY:

                    // GET ALL CONTACTS
                    List<String> contactPhonenumbers = new ArrayList<String>(); // TODO Rony use the contactsUtils Method
                    Cursor curPhones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                    assert curPhones != null;
                    while (curPhones.moveToNext())
                    {
                        String phoneNumber = curPhones.getString(curPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        contactPhonenumbers.add(PhoneNumberUtils.toValidPhoneNumber(phoneNumber));
                    }
                    curPhones.close();

                    if(contactPhonenumbers.contains(PhoneNumberUtils.toValidPhoneNumber(incomingNumber)))
                        return false;
                    else
                        return true;


                case PermissionBlockListLevel.BLACK_LIST_SPECIFIC:

                    Set<String> blockedSet = SharedPrefUtils.getStringSet(context, SharedPrefUtils.SETTINGS, SharedPrefUtils.BLOCK_LIST);
                    if (!blockedSet.isEmpty()) {
                        incomingNumber = PhoneNumberUtils.toValidPhoneNumber(incomingNumber);

                        if (blockedSet.contains(incomingNumber)) {
                            Log.i(TAG, "NUMBER MC BLOCKED: " + incomingNumber);
                            return true;
                        }
                    }
                    else {
                        Log.w(TAG, "BlackList empty allowing phone number: " + incomingNumber);
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
        SharedPrefUtils.setString(context, SharedPrefUtils.RADIO_BUTTON_SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME, PermissionBlockListLevel.BLACK_LIST_SPECIFIC);
    }

}
