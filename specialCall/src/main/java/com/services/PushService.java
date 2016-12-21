package com.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import com.app.AppStateManager;
import com.batch.android.Batch;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.receivers.PushReceiver;
import com.utils.BroadcastUtils;
import com.utils.PendingDownloadsUtils;
import com.utils.MCBlockListUtils;
import com.utils.NetworkingUtils;
import com.utils.SharedPrefUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.data.objects.DataKeys;
import com.data.objects.PushEventKeys;
import com.event.EventReport;
import com.event.EventType;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 05/02/2016.
 */
public class PushService extends IntentService {

    private static final String TAG = PushService.class.getSimpleName();

    private static final Type HASHMAP_TYPE = new TypeToken<Map<DataKeys, Object>>() { }.getType();

    public PushService() {
        super(PushService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        try {
            if (!Batch.Push.shouldDisplayPush(this, intent)) // Check that the push is valid
            {
                String errMsg = "Invalid push data! Push data was null. Terminating push receive";
                log(Log.ERROR,TAG, errMsg);
                throw new Exception(errMsg);
            }

            String alert = intent.getStringExtra(Batch.Push.ALERT_KEY);
            //BatchPushData pushData = new BatchPushData(this, intent);

            String eventActionCode = intent.getStringExtra(PushEventKeys.PUSH_EVENT_ACTION);
            log(Log.INFO,TAG, "PushEventActionCode:" + eventActionCode);

            switch (eventActionCode) {
                case PushEventKeys.PENDING_DOWNLOAD:
                    pendingDownload(intent);
                break;

                case PushEventKeys.TRANSFER_SUCCESS:
                    transferSuccess(intent, alert);
                break;

                case PushEventKeys.CLEAR_MEDIA:
                    clearMedia(intent);
                break;

                case PushEventKeys.CLEAR_SUCCESS:
                    clearSuccess(intent, alert);
                break;

                case PushEventKeys.SHOW_MESSAGE:
                    displayNotification(this, intent, EventType.DISPLAY_MESSAGE);
                break;

                case PushEventKeys.SHOW_ERROR:
                    displayNotification(this, intent, EventType.DISPLAY_ERROR);
                break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            PushReceiver.completeWakefulIntent(intent);
        }
    }

    //region Event action methods
    private void pendingDownload(Intent intent) {

        log(Log.INFO,TAG, "In:" + PushEventKeys.PENDING_DOWNLOAD);
        String jsonData = intent.getStringExtra(PushEventKeys.PUSH_EVENT_DATA);
        HashMap transferDetails = new Gson().fromJson(jsonData, HASHMAP_TYPE);
        String sourceId = (String)transferDetails.get(DataKeys.SOURCE_ID);

        if (MCBlockListUtils.IsMCBlocked(sourceId, getApplicationContext())) // Don't download if the number is blocked , just break and don't continue with the download flow
        {
            log(Log.WARN,TAG, "Number blocked for download:" + sourceId);
            return;
        }

        boolean isDownloadOnWifiOnly = SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SETTINGS, SharedPrefUtils.DOWNLOAD_ONLY_ON_WIFI);
        if(isDownloadOnWifiOnly)
        {
            if(NetworkingUtils.isWifiConnected(this)) {
                PendingDownloadsUtils.sendActionDownload(this, transferDetails);
            }
            else // Enqueuing pending download for later
            {
                PendingDownloadsUtils.enqueuePendingDownload(this, transferDetails);
            }
        }
        else {
            PendingDownloadsUtils.sendActionDownload(this, transferDetails);
        }
    }

    private void transferSuccess(Intent intent, String alert) {

        log(Log.INFO,TAG, "In:" + PushEventKeys.TRANSFER_SUCCESS);
        String jsonData = intent.getStringExtra(PushEventKeys.PUSH_EVENT_DATA);
        HashMap transferDetails = new Gson().fromJson(jsonData, HASHMAP_TYPE);

        BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.DESTINATION_DOWNLOAD_COMPLETE, null, transferDetails));
        displayNotificationInBgOnly(this, intent);
    }

    private void clearMedia(Intent intent) {

        log(Log.INFO,TAG, "In:" + PushEventKeys.CLEAR_MEDIA);
        String jsonData = intent.getStringExtra(PushEventKeys.PUSH_EVENT_DATA);
        HashMap transferDetails = new Gson().fromJson(jsonData, HASHMAP_TYPE);
        Intent i = new Intent(getApplicationContext(), ClearMediaIntentService.class);
        i.putExtra(ClearMediaIntentService.TRANSFER_DETAILS, transferDetails);
        startService(i);
    }

    private void clearSuccess(Intent intent, String alert) {

        log(Log.INFO,TAG, "In:" + PushEventKeys.CLEAR_SUCCESS);
        String jsonData = intent.getStringExtra(PushEventKeys.PUSH_EVENT_DATA);
        HashMap transferDetails = new Gson().fromJson(jsonData, HASHMAP_TYPE);
        BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.CLEAR_SUCCESS, null, transferDetails));
        displayNotificationInBgOnly(this, intent);
    }

    private void displayNotification(Context context, Intent intent, EventType eventType) {

        log(Log.INFO,TAG, "In: displayNotification");
        boolean isAppInForeground = AppStateManager.isAppInForeground(context);
        String appState = AppStateManager.getAppState(context);

        log(Log.INFO,TAG, String.format("[isAppInForeground]: %1$b, [App state]: %2$s", isAppInForeground, appState));

        if (isAppInForeground && AppStateManager.isLoggedIn(context)) {
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }

            String jsonData = intent.getStringExtra(PushEventKeys.PUSH_EVENT_DATA);
            HashMap data = new Gson().fromJson(jsonData, HASHMAP_TYPE);

            String msg = (String) data.get(DataKeys.HTML_STRING);
            EventReport eventReport = new EventReport(eventType, msg, null);

            BroadcastUtils.sendEventReportBroadcast(context, TAG, eventReport);
        }
        else
            Batch.Push.displayNotification(this, intent);
    }

    private void displayNotificationInBgOnly(Context context, Intent intent) {

        log(Log.INFO,TAG, "In: displayNotificationInBgOnly");
        boolean isAppInForeground = AppStateManager.isAppInForeground(context);
        String appState = AppStateManager.getAppState(context);

        log(Log.INFO,TAG, String.format("[isAppInForeground]: %1$b, [App state]: %2$s", isAppInForeground, appState));

        if (isAppInForeground && AppStateManager.isLoggedIn(context)) {
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(AppStateManager.isLoggedIn(context))
            Batch.Push.displayNotification(this, intent);
    }
    //endregion
}

