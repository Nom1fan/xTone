package com.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.special.app.R;
import com.ui.activities.MainActivity;
import com.ui.components.NotificationHelper;

/**
 * Created by mor on 18/10/2015.
 */
public abstract class NotificationUtils {

    private static final String TAG = NotificationUtils.class.getSimpleName();
    private static final int MAX_NOTIF_NUM = 5;
    public static int NUM_OF_NOTIF = -1;
    private static NotificationHelper[] mNotificationHelpersArr = new NotificationHelper[MAX_NOTIF_NUM];
    public static final int FOREGROUND_NOTIFICATION_ID = 666;

    public static void createHelper(Context context, String initialMsg) {

        NUM_OF_NOTIF = (NUM_OF_NOTIF + 1) % MAX_NOTIF_NUM;
        Log.i(TAG, "NUM_OF_NOTIF=" + NUM_OF_NOTIF);
        mNotificationHelpersArr[NUM_OF_NOTIF] = new NotificationHelper(NUM_OF_NOTIF);

        mNotificationHelpersArr[NUM_OF_NOTIF].createUploadNotification(context, initialMsg);
    }

    public static NotificationHelper getNextHelper() {

        return mNotificationHelpersArr[NUM_OF_NOTIF];
    }

    public static void freeHelperSpace() {

        mNotificationHelpersArr[NUM_OF_NOTIF] = null;
        NUM_OF_NOTIF--;
    }

    public static Notification getCompatNotification(Context context){

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.notification_mc)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.color_mc))
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_text))
                .setTicker(context.getString(R.string.notification_ticker_text))
                .setWhen((System.currentTimeMillis()));
        Intent StartIntent = new Intent (context,MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context,FOREGROUND_NOTIFICATION_ID,StartIntent,0);
        builder.setContentIntent(contentIntent);
        Notification notification = builder.build();
        return notification;
    }
}
