package com.services;

import android.content.Context;
import android.content.Intent;

import com.app.AppStateManager;
import com.data.objects.DefaultMediaData;
import com.data.objects.PendingDownloadData;
import com.data.objects.PushEventKeys;
import com.enums.SpecialMediaType;
import com.mediacallz.app.R;

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
