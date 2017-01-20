package com.handlers.push.service;

import android.content.Context;
import android.util.Log;

import com.data.objects.PendingDownloadData;
import com.handlers.AbstractPushHandler;
import com.utils.MCBlockListUtils;
import com.utils.NetworkingUtils;
import com.utils.PendingDownloadsUtils;
import com.utils.SharedPrefUtils;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 12/31/2016.
 */

public class PushPendingDownloadHandler extends AbstractPushHandler {

    public static final String TAG = PushPendingDownloadHandler.class.getSimpleName();

    @Override
    public void handlePush(Context ctx, String pushData, Object ... extraParams) {
        PendingDownloadData pendingDownloadData = gson.fromJson(pushData, PendingDownloadData.class);
        String sourceId = pendingDownloadData.getSourceId();

        if (MCBlockListUtils.IsMCBlocked(sourceId, ctx)) // Don't download if the number is blocked , just break and don't continue with the download flow
        {
            log(Log.WARN,TAG, "Number blocked for download:" + sourceId);
            return;
        }

        boolean isDownloadOnWifiOnly = SharedPrefUtils.getBoolean(ctx, SharedPrefUtils.SETTINGS, SharedPrefUtils.DOWNLOAD_ONLY_ON_WIFI);
        if(isDownloadOnWifiOnly)
        {
            if(NetworkingUtils.isWifiConnected(ctx)) {
                PendingDownloadsUtils.sendActionDownload(ctx, pendingDownloadData);
            }
            else // Enqueuing pending download for later
            {
                PendingDownloadsUtils.enqueuePendingDownload(ctx, pendingDownloadData);
            }
        }
        else {
            PendingDownloadsUtils.sendActionDownload(ctx, pendingDownloadData);
        }
    }
}
