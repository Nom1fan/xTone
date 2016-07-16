package com.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.data_objects.Constants;
import com.utils.SharedPrefUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.SpecialMediaType;
import FilesManager.FileManager;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 18/02/2016.
 */
public class ClearMediaIntentService extends IntentService {

    public static final String TAG = ClearMediaIntentService.class.getSimpleName();
    public static final String TRANSFER_DETAILS = "TransferDetails";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ClearMediaIntentService(String name) {
        super(name);
    }

    public ClearMediaIntentService() { super("ClearMediaIntentService"); }

    @Override
    protected void onHandleIntent(Intent intent) {

        log(Log.INFO,TAG, "Handling intent");
        if(intent!=null) {

            Map data = (Map) intent.getSerializableExtra(TRANSFER_DETAILS);
            SpecialMediaType specialMediaType = SpecialMediaType.valueOf(data.get(DataKeys.SPECIAL_MEDIA_TYPE).toString());
            String phoneNumber = data.get(DataKeys.SOURCE_ID).toString();

            try {
                String folderPath;
                switch (specialMediaType) {

                    case CALLER_MEDIA:
                        log(Log.INFO,TAG, "Clearing CALLER_MEDIA");
                        SharedPrefUtils.remove(getApplicationContext(), SharedPrefUtils.CALLER_MEDIA_FILEPATH, phoneNumber);
                        SharedPrefUtils.remove(getApplicationContext(), SharedPrefUtils.RINGTONE_FILEPATH, phoneNumber);
                        folderPath = Constants.INCOMING_FOLDER + phoneNumber;
                        FileManager.deleteDirectory(new File(folderPath));
                        break;
                    case PROFILE_MEDIA:
                        log(Log.INFO,TAG, "Clearing PROFILE_MEDIA");
                        SharedPrefUtils.remove(getApplicationContext(), SharedPrefUtils.PROFILE_MEDIA_FILEPATH, phoneNumber);
                        SharedPrefUtils.remove(getApplicationContext(), SharedPrefUtils.FUNTONE_FILEPATH, phoneNumber);
                        folderPath = Constants.OUTGOING_FOLDER + phoneNumber;
                        FileManager.deleteDirectory(new File(folderPath));
                        break;

                    case MY_DEFAULT_PROFILE_MEDIA:
                        //TODO Not yet implemented
                        break;

                    case MY_DEFAULT_CALLER_MEDIA:
                        //TODO Not yet implemented
                        break;

                }

                // Notifying clear requester that media was successfully cleared
                Intent i = new Intent(getApplicationContext(), StorageServerProxyService.class);
                i.setAction(StorageServerProxyService.ACTION_NOTIFY_MEDIA_CLEARED);
                i.putExtra(StorageServerProxyService.TRANSFER_DETAILS, (Serializable) data);
                startService(i);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {

                //TODO Mor: Inform clear requester that clear may have failed
                log(Log.ERROR,TAG, "Unable to clear media from user:"+phoneNumber+". [Exception]:" + (e.getMessage()!=null ? e.getMessage() : e));
            }
        }

    }
}
