package com.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.logger.Logger;
import com.logger.LoggerFactory;
import com.receivers.SyncDefaultMediaReceiver;
import com.utils.ContactsUtils;
import com.utils.UtilityFactory;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 25/05/2017.
 */

public class SyncOnDefaultMediaIntentService extends IntentService {

    private static final String TAG = SyncOnDefaultMediaIntentService.class.getSimpleName();

    private final Logger logger = LoggerFactory.getLogger();


    public SyncOnDefaultMediaIntentService() {
        super(TAG);
    }

    public SyncOnDefaultMediaIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
       logger.debug(TAG, "Initiating default media sync process...");

        if(intent != null) {
            SyncOnDefaultMediaIntentServiceLogic logic = new SyncOnDefaultMediaIntentServiceLogic(this, new ServerProxyAccess());

            logic.executeLogic();
            SyncDefaultMediaReceiver.completeWakefulIntent(intent);
        }
    }
}
