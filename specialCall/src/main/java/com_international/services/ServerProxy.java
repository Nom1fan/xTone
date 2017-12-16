package com_international.services;

import android.content.Context;

import com_international.data.objects.DefaultMediaData;
import com_international.data.objects.PendingDownloadData;
import com_international.enums.SpecialMediaType;

/**
 * Created by Mor on 31/05/2017.
 */

public interface ServerProxy {

    void notifyMediaReady(Context context, PendingDownloadData pendingDownloadData);

    void sendActionDownload(Context context, PendingDownloadData pendingDownloadData);

    Void sendActionDownload(Context context, PendingDownloadData pendingDownloadData, DefaultMediaData defaultMediaData);

    void register(Context context, int smsVerificationCode);

    void clearMedia(Context context, String destPhoneNumber, SpecialMediaType spMediaType);

    void getRegisteredContacts(Context context);
}
