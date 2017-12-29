package com.utils;

import android.content.Context;

import com.data.objects.CallHistoryRecord;
import com.data.objects.ContactWrapper;

import java.util.List;

/**
 * Created by Mor on 25/02/2016.
 */
public abstract class CacheUtils {

    public static List<CallHistoryRecord> cachedCallHistoryList;

    public static List<ContactWrapper> cachedContactList;

    public static boolean isPhoneInCache(Context context, String destPhone) {

        String destPhoneCache = SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LAST_CHECKED_NUMBER_CACHE);
        return destPhone.equals(destPhoneCache);
    }

    public static void setPhone(Context context, String destPhone) {

        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LAST_CHECKED_NUMBER_CACHE, destPhone);
    }

}
