package com.services;

import android.content.Context;

import com.data.objects.DefaultMediaData;
import com.data.objects.PendingDownloadData;
import com.enums.SpecialMediaType;

/**
 * Created by Mor on 31/05/2017.
 */

public class ServerProxyAccess implements ServerProxy {
    @Override
    public void notifyMediaReady(Context context, PendingDownloadData pendingDownloadData) {
        ServerProxyService.notifyMediaReady(context, pendingDownloadData);
    }

    @Override
    public void sendActionDownload(Context context, PendingDownloadData pendingDownloadData) {
        ServerProxyService.sendActionDownload(context, pendingDownloadData);
    }

    @Override
    public Void sendActionDownload(Context context, PendingDownloadData pendingDownloadData, DefaultMediaData defaultMediaData) {
        ServerProxyService.sendActionDownload(context, pendingDownloadData, defaultMediaData);
        return null;
    }

    @Override
    public void register(Context context, int smsVerificationCode) {
        ServerProxyService.register(context, smsVerificationCode);
    }

    @Override
    public void clearMedia(Context context, String destPhoneNumber, SpecialMediaType spMediaType) {
        ServerProxyService.clearMedia(context, destPhoneNumber, spMediaType);
    }

    @Override
    public void getRegisteredContacts(Context context) {
        ServerProxyService.getRegisteredContacts(context);
    }
}
