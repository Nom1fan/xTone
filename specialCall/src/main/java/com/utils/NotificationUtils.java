package com.utils;

import android.content.Context;
import android.util.Log;

import com.ui.components.NotificationHelper;

/**
 * Created by mor on 18/10/2015.
 */
public class NotificationUtils {

    private static final String TAG = NotificationUtils.class.getSimpleName();
    private static final int MAX_NOTIF_NUM = 5;
    public static int NUM_OF_NOTIF = -1;
    private static NotificationHelper[] mNotificationHelpersArr = new NotificationHelper[MAX_NOTIF_NUM];

    public static void createHelper(Context context, String initialMsg) {

        NUM_OF_NOTIF=(NUM_OF_NOTIF+1)%MAX_NOTIF_NUM;
        Log.i(TAG, "NUM_OF_NOTIF="+NUM_OF_NOTIF);
        mNotificationHelpersArr[NUM_OF_NOTIF] = new NotificationHelper(context,NUM_OF_NOTIF);

        mNotificationHelpersArr[NUM_OF_NOTIF].createUploadNotification(initialMsg);
    }

    public static NotificationHelper getNextHelper() {

        return mNotificationHelpersArr[NUM_OF_NOTIF];
    }

    public static void freeHelperSpace() {

        mNotificationHelpersArr[NUM_OF_NOTIF] = null;
        NUM_OF_NOTIF--;
    }
}
