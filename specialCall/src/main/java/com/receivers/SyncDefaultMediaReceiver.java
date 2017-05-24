package com.receivers;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.app.AppStateManager;
import com.client.ClientFactory;
import com.client.MediaClient;
import com.data.objects.Constants;
import com.data.objects.Contact;
import com.data.objects.DefaultMediaData;
import com.data.objects.PendingDownloadData;
import com.enums.SpecialMediaType;
import com.files.media.MediaFile;
import com.services.IncomingService;
import com.services.OutgoingService;
import com.services.ServerProxyService;
import com.utils.BroadcastUtils;
import com.utils.ContactsUtils;
import com.utils.MediaFilesUtils;
import com.utils.PhoneNumberUtils;
import com.utils.SharedPrefUtils;

import java.io.File;
import java.util.List;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by rony on 01/03/2016.
 */

public class SyncDefaultMediaReceiver extends WakefulBroadcastReceiver {

    public static final String SYNC_ACTION = "com.android.mediacallz.SYNC_DEFULAT_MEDIA_ACTION";

    private static final String TAG = SyncDefaultMediaReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (AppStateManager.isLoggedIn(context)) {
            String action = intent.getAction();
            log(Log.DEBUG, TAG, "onReceive ACTION INTENT:" + action);

            List<Contact> allContacts = ContactsUtils.getAllContacts(context);
            MediaClient mediaClient = ClientFactory.getMediaClient(context);

            for (Contact contact : allContacts) {
                List<DefaultMediaData> defaultMediaDataList = mediaClient.getDefaultMediaData(contact.getPhoneNumber(), SpecialMediaType.DEFAULT_CALLER_MEDIA);
                String defaultCallerMediaDir = Constants.DEFAULT_INCOMING_FOLDER + "/" + contact.getPhoneNumber();
                File[] files = new File(defaultCallerMediaDir).listFiles();

                syncFiles(context, defaultMediaDataList, files);
            }
        }
    }

    protected void syncFiles(Context context, List<DefaultMediaData> defaultMediaDataList, File[] files) {
        if(files != null) {
            for (File file : files) {
                syncFile(context, defaultMediaDataList, file);
            }
        }
    }

    private void syncFile(Context context, List<DefaultMediaData> defaultMediaDataList, File file) {
        MediaFile.FileType fileType = MediaFilesUtils.getFileType(file);
        for (DefaultMediaData defaultMediaData : defaultMediaDataList) {
            if(defaultMediaData.getFileType().equals(fileType)) {
                long savedFileUnixTime = MediaFilesUtils.getFileCreationDateInUnixTime(file);
                long latestFileMediaUnixTime = defaultMediaData.getDefaultMediaUnixTime();

                // Need to sync
                if(latestFileMediaUnixTime > savedFileUnixTime) {
                    PendingDownloadData pendingDownloadData = getPendingDownloadData(defaultMediaData);
                    ServerProxyService.sendActionDownload(context, pendingDownloadData);
                }
            }
        }
    }

    @NonNull
    private PendingDownloadData getPendingDownloadData(DefaultMediaData defaultMediaData) {
        PendingDownloadData pendingDownloadData = new PendingDownloadData();
        pendingDownloadData.setSpecialMediaType(SpecialMediaType.DEFAULT_CALLER_MEDIA);
        pendingDownloadData.setFilePathOnServer(defaultMediaData.getFilePathOnServer());
        return pendingDownloadData;
    }

}