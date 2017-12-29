package com.utils;

import android.content.Context;

/**
 * Created by Mor on 22/05/2017.
 */

public abstract class MediaCallSessionUtils {

    public static void setIncomingRingingSession(Context context, boolean val) {
        SharedPrefUtils.setBoolean(context, SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_RINGING_SESSION, val);
    }

    public static boolean isIncomingRingingInSession(Context context) {
        return SharedPrefUtils.getBoolean(context, SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_RINGING_SESSION);
    }

    public static void setOutgoingRingingSession(Context context, boolean val) {
        SharedPrefUtils.setBoolean(context, SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_RINGING_SESSION, val);
    }

    public static boolean isOutgoingRingingInSession(Context context) {
        return SharedPrefUtils.getBoolean(context, SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_RINGING_SESSION);
    }
}
