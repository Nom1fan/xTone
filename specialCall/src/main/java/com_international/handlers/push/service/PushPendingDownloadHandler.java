package com_international.handlers.push.service;

import android.content.Context;
import android.util.Log;

import com_international.data.objects.PendingDownloadData;
import com_international.handlers.AbstractPushHandler;
import com_international.services.ServerProxyService;
import com_international.utils.MCBlockListUtils;
import com_international.utils.NetworkingUtils;
import com_international.utils.PendingDownloadsUtils;
import com_international.utils.SettingsUtils;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 12/31/2016.
 */

public class PushPendingDownloadHandler extends AbstractPushHandler {

    public static final String TAG = PushPendingDownloadHandler.class.getSimpleName();

    @Override
    public void handlePush(Context context, String pushData, Object ... extraParams) {
        PendingDownloadData pendingDownloadData = gson.fromJson(pushData, PendingDownloadData.class);
        String sourceId = pendingDownloadData.getSourceId();

        if (MCBlockListUtils.IsMCBlocked(sourceId, context)) // Don't download if the number is blocked , just break and don't continue with the download flow
        {
            log(Log.WARN,TAG, "Number blocked for download:" + sourceId);
            return;
        }

        boolean isDownloadOnWifiOnly = SettingsUtils.isDownloadOnlyOnWifi(context);
        if(isDownloadOnWifiOnly)
        {
            if(NetworkingUtils.isWifiConnected(context)) {
                ServerProxyService.sendActionDownload(context, pendingDownloadData);
            }
            else // Enqueuing pending download for later
            {
                PendingDownloadsUtils.enqueuePendingDownload(context, pendingDownloadData);
            }
        }
        else {
            ServerProxyService.sendActionDownload(context, pendingDownloadData);
        }
    }
}
