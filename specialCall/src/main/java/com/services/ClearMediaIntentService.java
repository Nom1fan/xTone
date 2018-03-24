package com.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.data.objects.ClearMediaData;
import com.data.objects.Constants;
import com.enums.SpecialMediaType;
import com.logger.Logger;
import com.logger.LoggerFactory;
import com.utils.Phone2MediaPathMapperUtils;
import com.utils.UtilityFactory;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by Mor on 18/02/2016.
 */
public class ClearMediaIntentService extends IntentService {
    
    private static final String TAG = ClearMediaIntentService.class.getSimpleName();

    private final Logger logger = LoggerFactory.getLogger();
    
    public static final String CLEAR_MEDIA_DATA = "CLEAR_MEDIA_DATA";

    private Phone2MediaPathMapperUtils phone2MediaPathMapperUtils = UtilityFactory.instance().getUtility(Phone2MediaPathMapperUtils.class);
    

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
        logger.info(TAG, "Handling intent");
        Context context = getApplicationContext();

        if (intent != null) {

            ClearMediaData data = (ClearMediaData) intent.getSerializableExtra(CLEAR_MEDIA_DATA);
            SpecialMediaType specialMediaType = data.getSpecialMediaType();
            String phoneNumber = data.getSourceId();
            phoneNumber = phoneNumber.substring(phoneNumber.length() - 6); /// TODO:  ADDED THIS FOR INTERNATIONAL OPTIONS (HACKED)
            try {
                String folderPath = null;
                switch (specialMediaType) {

                    case CALLER_MEDIA:
                        logger.info(TAG, "Clearing CALLER_MEDIA");
                        phone2MediaPathMapperUtils.removeCallerVisualMediaPath(context, phoneNumber);
                        phone2MediaPathMapperUtils.removeCallerAudioMediaPath(context, phoneNumber);
                        folderPath = Constants.INCOMING_FOLDER + phoneNumber;
                        break;
                    case PROFILE_MEDIA:
                        logger.info(TAG, "Clearing PROFILE_MEDIA");
                        phone2MediaPathMapperUtils.removeProfileVisualMediaPath(context, phoneNumber);
                        phone2MediaPathMapperUtils.removeProfileAudioMediaPath(context, phoneNumber);
                        folderPath = Constants.OUTGOING_FOLDER + phoneNumber;
                        break;

                    case DEFAULT_CALLER_MEDIA:
                        logger.info(TAG, "Clearing DEFAULT_CALLER_MEDIA");
                        phone2MediaPathMapperUtils.removeDefaultCallerVisualMediaPath(context, phoneNumber);
                        phone2MediaPathMapperUtils.removeDefaultCallerAudioMediaPath(context, phoneNumber);
                        folderPath = Constants.DEFAULT_INCOMING_FOLDER + phoneNumber;
                        break;
                    
                    case DEFAULT_PROFILE_MEDIA:
                        logger.info(TAG, "Clearing DEFAULT_PROFILE_MEDIA");
                        phone2MediaPathMapperUtils.removeProfileVisualMediaPath(context, phoneNumber);
                        phone2MediaPathMapperUtils.removeProfileAudioMediaPath(context, phoneNumber);
                        folderPath = Constants.DEFAULT_OUTGOING_FOLDER + phoneNumber;
                        break;


                }
                FileUtils.deleteDirectory(new File(folderPath));

                // Notifying clear requester that media was successfully cleared
                Intent i = new Intent(context, ServerProxyService.class);
                i.setAction(ServerProxyService.ACTION_NOTIFY_MEDIA_CLEARED);
                i.putExtra(ServerProxyService.CLEAR_MEDIA_DATA, data);
                startService(i);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                //TODO Mor: Inform clear requester that clear may have failed
                logger.error(TAG, "Unable to clear media from user:" + phoneNumber + ". [Exception]:" + (e.getMessage() != null ? e.getMessage() : e));
            }
        }

    }
}
