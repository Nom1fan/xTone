package com.utils;

import android.app.Notification;
import android.app.NotificationManager;
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
import com.data.objects.PushNotificationData;
import com.event.EventReport;
import com.event.EventType;
import com.google.firebase.messaging.RemoteMessage;
import com.mediacallz.app.R;
import com.ui.activities.MainActivity;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by mor on 18/10/2015.
 */
public abstract class NotificationUtils {

    private static final String TAG = NotificationUtils.class.getSimpleName();
    public static final int FOREGROUND_NOTIFICATION_ID = 666;

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
        return builder.build();
    }

    public static void displayNotificationInBgOnly(Context context, RemoteMessage.Notification notification) {

        log(Log.INFO, TAG, "In: displayNotificationInBgOnly");
        boolean isAppInForeground = AppStateManager.isAppInForeground(context);
        String appState = AppStateManager.getAppState(context);

        log(Log.INFO, TAG, String.format("[isAppInForeground]: %1$b, [App state]: %2$s", isAppInForeground, appState));

        if (isAppInForeground && AppStateManager.isLoggedIn(context)) {
            try {
                Uri ring = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(context, ring);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (AppStateManager.isLoggedIn(context)) {
            sendNotification(context, notification);
        }
    }

    public static void displayNotification(Context ctx, PushNotificationData pushNotificationData, RemoteMessage.Notification notification, EventType eventType) {
        boolean isAppInForeground = AppStateManager.isAppInForeground(ctx);
        String appState = AppStateManager.getAppState(ctx);

        log(Log.INFO, TAG, String.format("[isAppInForeground]: %1$b, [App state]: %2$s", isAppInForeground, appState));

        if (isAppInForeground && AppStateManager.isLoggedIn(ctx)) {
            playNotificationSound(ctx);
            String msg = pushNotificationData.getHtmlString();
            EventReport eventReport = new EventReport(eventType, msg);
            BroadcastUtils.sendEventReportBroadcast(ctx, TAG, eventReport);
        } else {
            sendNotification(ctx, notification);
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

    /**
     * Create and show a simple notification with title and message
     */
    public static void sendNotification(Context context, String title, String msg) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.color_mc))
                        .setSmallIcon(R.drawable.color_mc)
                        .setContentTitle(title)
                        .setContentText(msg)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        assert  notificationManager != null;

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    public static void sendNotification(Context context, RemoteMessage.Notification notification) {
        sendNotification(context, notification.getTitle(), notification.getBody());
    }
}
