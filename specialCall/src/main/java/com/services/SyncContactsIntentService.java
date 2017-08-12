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

public class SyncContactsIntentService extends IntentService {

    private static final String TAG = SyncContactsIntentService.class.getSimpleName();

    private final Logger logger = LoggerFactory.getLogger();

    public SyncContactsIntentService() {
        super(TAG);
    }

    public SyncContactsIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        logger.info(TAG, "Inside SyncContactsIntentService");

        if (intent != null) {
            SyncContactsLogic logic = new SyncContactsLogic(getApplicationContext());
            logic.executeLogic();
        }
    }
}
