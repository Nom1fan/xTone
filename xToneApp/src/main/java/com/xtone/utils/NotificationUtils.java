package com.xtone.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.NotificationCompat;

import com.google.firebase.messaging.RemoteMessage;
import com.xtone.app.R;
import com.xtone.app.xToneApp;
import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;

/**
 * Created by mor on 18/10/2015.
 */
public abstract class NotificationUtils {

    private static final Logger log = LoggerFactory.getLogger();

    private static final String TAG = NotificationUtils.class.getSimpleName();

    public static final int FOREGROUND_NOTIFICATION_ID = 666;

//    public static Notification getCompatNotification(Context context) {
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
//        builder.setSmallIcon(R.drawable.notification_mc)
//                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.color_mc))
//                .setContentTitle(context.getString(R.string.notification_title))
//                .setContentText(context.getString(R.string.notification_text))
//                .setTicker(context.getString(R.string.notification_ticker_text))
//                .setWhen((System.currentTimeMillis()));
//        Intent StartIntent = new Intent(context, MainActivity.class);
//        PendingIntent contentIntent = PendingIntent.getActivity(context, FOREGROUND_NOTIFICATION_ID, StartIntent, 0);
//        builder.setContentIntent(contentIntent);
//        return builder.build();
//    }

    public static void playNotificationSound(Context ctx) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(ctx, notification);
            r.play();
        } catch (Exception e) {
            log.warn(TAG, String.format("Could not play push notification sound. Exception%s", e));
        }
    }

    /**
     * Create and show a simple notification with title and message
     */
    public static void sendNotification(Context context, String title, String msg) {
        Intent intent = new Intent(context, xToneApp.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(context)
//                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.color_mc))
//                        .setSmallIcon(R.drawable.color_mc)
                        .setContentTitle(title)
                        .setContentText(msg)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        assert notificationManager != null;

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    public static void sendNotification(Context context, RemoteMessage.Notification notification) {
        sendNotification(context, notification.getTitle(), notification.getBody());
    }
}
