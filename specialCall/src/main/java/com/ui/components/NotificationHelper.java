package com.ui.components;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.special.app.R;


public class NotificationHelper {
    private Context mContext;
    private int NOTIFICATION_ID;
    private Notification mNotification;
    private NotificationManager mNotificationManager;
    private PendingIntent mContentIntent;
    private CharSequence mContentTitle;

    public NotificationHelper(Context context, int notificationId)
    {
        mContext = context;
        NOTIFICATION_ID = notificationId;
    }


    /**
     * Create and display upload notification
     */
    public void createUploadNotification(String initialMsg) {

        //get the notification manager
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        //create the notification
        int icon = android.R.drawable.stat_sys_upload;
        CharSequence tickerText = mContext.getString(R.string.upload_ticker); //Initial text that appears in the status bar
        long when = System.currentTimeMillis();
        mNotification = new Notification(icon, tickerText, when);

        //create the content which is shown in the notification pulldown
        mContentTitle = mContext.getString(R.string.content_title); //Full title of the notification in the pull down
        CharSequence contentText = initialMsg; //Text of the notification in the pull down

        //you have to set a PendingIntent on a notification to tell the system what you want it to do when the notification is selected
        //I don't want to use this here so I'm just creating a blank one
        Intent notificationIntent = new Intent();
        mContentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

        //add the additional content and intent to the notification
        mNotification.setLatestEventInfo(mContext, mContentTitle, contentText, mContentIntent);

        //make this notification appear in the 'Ongoing events' section
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;

        //show the notification
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);

    }

    /**
     * Receives progress updates from the background task and updates the status bar notification appropriately
     * @param text
     */
    public void progressUpdate(String text) {
        //build up the new status message
        CharSequence contentText = text;
        //publish it to the status bar
        mNotification.setLatestEventInfo(mContext, mContentTitle, contentText, mContentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    /**
     * called when the background task is complete, this removes the notification from the status bar.
     * We could also use this to add a new ‘task complete’ notification
     */
    public void completed()    {
        //remove the notification from the status bar
        mNotificationManager.cancel(NOTIFICATION_ID);
    }
}