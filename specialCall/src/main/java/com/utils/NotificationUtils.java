package com.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.app.AppStateManager;
import com.batch.android.Batch;
import com.crashlytics.android.Crashlytics;
import com.data.objects.PushNotificationData;
import com.event.EventReport;
import com.event.EventType;
import com.mediacallz.app.R;
import com.ui.activities.MainActivity;
import com.ui.components.NotificationHelper;

import static com.crashlytics.android.Crashlytics.*;

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
        log(Log.INFO, TAG, "NUM_OF_NOTIF=" + NUM_OF_NOTIF);
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

    public static Notification getCompatNotification(Context context) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.notification_mc)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.color_mc))
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_text))
                .setTicker(context.getString(R.string.notification_ticker_text))
                .setWhen((System.currentTimeMillis()));
        Intent StartIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, FOREGROUND_NOTIFICATION_ID, StartIntent, 0);
        builder.setContentIntent(contentIntent);
        Notification notification = builder.build();
        return notification;
    }

    public static void displayNotificationInBgOnly(Context context, Intent intent) {

        log(Log.INFO, TAG, "In: displayNotificationInBgOnly");
        boolean isAppInForeground = AppStateManager.isAppInForeground(context);
        String appState = AppStateManager.getAppState(context);

        log(Log.INFO, TAG, String.format("[isAppInForeground]: %1$b, [App state]: %2$s", isAppInForeground, appState));

        if (isAppInForeground && AppStateManager.isLoggedIn(context)) {
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(context, notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (AppStateManager.isLoggedIn(context))
            Batch.Push.displayNotification(context, intent);
    }

    public static void displayNotification(Context ctx, PushNotificationData pushNotificationData, Intent intent, EventType eventType) {
        boolean isAppInForeground = AppStateManager.isAppInForeground(ctx);
        String appState = AppStateManager.getAppState(ctx);

        log(Log.INFO, TAG, String.format("[isAppInForeground]: %1$b, [App state]: %2$s", isAppInForeground, appState));

        if (isAppInForeground && AppStateManager.isLoggedIn(ctx)) {
            playNotificationSound(ctx);
            String msg = pushNotificationData.getHtmlString();
            EventReport eventReport = new EventReport(eventType, msg);
            BroadcastUtils.sendEventReportBroadcast(ctx, TAG, eventReport);
        } else {
            Batch.Push.displayNotification(ctx, intent);
        }
    }

    public static void playNotificationSound(Context ctx) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(ctx, notification);
            r.play();
        } catch (Exception e) {
            log(Log.WARN, TAG, "Could not play push notification sound");
            e.printStackTrace();
        }
    }
}
