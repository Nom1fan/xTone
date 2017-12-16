package com_international.ui.components;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com_international.mediacallz.app.R;


public class NotificationHelper {
    private int NOTIFICATION_ID;
    private NotificationManager mNotificationManager;
    private PendingIntent mContentIntent;
    private CharSequence mContentTitle;

    public NotificationHelper(int notificationId) {
        NOTIFICATION_ID = notificationId;
    }


    /**
     * Create and display upload notification
     */
    public void createUploadNotification(Context context, String initialMsg) {

        // Get the notification manager
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //create the notification
        int icon = android.R.drawable.stat_sys_upload;
        CharSequence tickerText = context.getString(R.string.upload_ticker); //Initial text that appears in the status bar
        long when = System.currentTimeMillis();

        // Create the content which is shown in the notification pulldown
        mContentTitle = context.getString(R.string.content_title); //Full title of the notification in the pull down
        CharSequence contentText = initialMsg; //Text of the notification in the pull down

        // You have to set a PendingIntent on a notification to tell the system what you want it to do when the notification is selected
        // I don't want to use this here so I'm just creating a blank one
        Intent notificationIntent = new Intent();
        mContentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        //add the additional content and intent to the notification
        Notification.Builder builder = new Notification.Builder(context);
        builder.setContentTitle(mContentTitle);
        builder.setContentText(contentText);
        builder.setContentIntent(mContentIntent);
        builder.setWhen(when);
        builder.setTicker(tickerText);
        builder.setSmallIcon(icon);
        builder.setOngoing(true);

        //show the notification
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());

    }

    /**
     * Receives progress updates from the background task and updates the status bar notification appropriately
     *
     * @param text
     */
    public void progressUpdate(Context context, String text) {
        //build up the new status message
        CharSequence contentText = text;
        //publish it to the status bar
        Notification.Builder builder = new Notification.Builder(context);
        builder.setContentTitle(mContentTitle);
        builder.setContentText(contentText);
        builder.setContentIntent(mContentIntent);
        builder.setOngoing(true);

        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * called when the background task is complete, this removes the notification from the status bar.
     * We could also use this to add a new ‘task complete’ notification
     */
    public void completed() {
        //remove the notification from the status bar
        mNotificationManager.cancel(NOTIFICATION_ID);
    }
}