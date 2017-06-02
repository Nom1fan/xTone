package com.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.data.objects.ClearMediaData;
import com.data.objects.Constants;
import com.utils.MediaFileUtils;
import com.utils.Phone2MediaMapperUtils;

import java.io.File;
import java.io.FileNotFoundException;

import com.enums.SpecialMediaType;
import com.utils.UtilityFactory;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 18/02/2016.
 */
public class ClearMediaIntentService extends IntentService {

    public static final String TAG = ClearMediaIntentService.class.getSimpleName();
    
    public static final String CLEAR_MEDIA_DATA = "CLEAR_MEDIA_DATA";

    private MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ClearMediaIntentService(String name) {
        super(name);
    }

    public ClearMediaIntentService() {
        super("ClearMediaIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        log(Log.INFO, TAG, "Handling intent");
        Context context = getApplicationContext();

        if (intent != null) {

            ClearMediaData data = (ClearMediaData) intent.getSerializableExtra(CLEAR_MEDIA_DATA);
            SpecialMediaType specialMediaType = data.getSpecialMediaType();
            String phoneNumber = data.getSourceId();

            try {
                String folderPath;
                switch (specialMediaType) {

                    case CALLER_MEDIA:
                        log(Log.INFO, TAG, "Clearing CALLER_MEDIA");
                        Phone2MediaMapperUtils.removeCallerVisualMedia(context, phoneNumber);
                        Phone2MediaMapperUtils.removeCallerAudioMedia(context, phoneNumber);
                        folderPath = Constants.INCOMING_FOLDER + phoneNumber;
                        mediaFileUtils.deleteDirectory(new File(folderPath));
                        break;
                    case PROFILE_MEDIA:
                        log(Log.INFO, TAG, "Clearing PROFILE_MEDIA");
                        Phone2MediaMapperUtils.removeProfileVisualMedia(context, phoneNumber);
                        Phone2MediaMapperUtils.removeProfileAudioMedia(context, phoneNumber);
                        folderPath = Constants.OUTGOING_FOLDER + phoneNumber;
                        mediaFileUtils.deleteDirectory(new File(folderPath));
                        break;

                    case DEFAULT_PROFILE_MEDIA:
                        //TODO Not yet implemented
                        break;

                    case DEFAULT_CALLER_MEDIA:
                        //TODO Not yet implemented
                        break;

                }

                // Notifying clear requester that media was successfully cleared
                Intent i = new Intent(context, ServerProxyService.class);
                i.setAction(ServerProxyService.ACTION_NOTIFY_MEDIA_CLEARED);
                i.putExtra(ServerProxyService.CLEAR_MEDIA_DATA, data);
                startService(i);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {

                //TODO Mor: Inform clear requester that clear may have failed
                log(Log.ERROR, TAG, "Unable to clear media from user:" + phoneNumber + ". [Exception]:" + (e.getMessage() != null ? e.getMessage() : e));
            }
        }

    }
}
