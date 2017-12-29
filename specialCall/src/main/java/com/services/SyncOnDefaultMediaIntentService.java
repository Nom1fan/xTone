package com.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.receivers.SyncDefaultMediaReceiver;
import com.utils.ContactsUtils;
import com.utils.ContactsUtilsImpl;
import com.utils.UtilityFactory;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 25/05/2017.
 */

public class SyncOnDefaultMediaIntentService extends IntentService {

    private static final String TAG = SyncOnDefaultMediaIntentService.class.getSimpleName();

    private final ContactsUtils contactsUtils = UtilityFactory.instance().getUtility(ContactsUtils.class);


    public SyncOnDefaultMediaIntentService() {
        super(TAG);
    }

    public SyncOnDefaultMediaIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        log(Log.DEBUG, TAG, "Initiating default media sync process...");

        if(intent != null) {
            SyncOnDefaultMediaIntentServiceLogic logic = new SyncOnDefaultMediaIntentServiceLogic(this, new ServerProxyAccess());

            logic.performSyncOnDefaultMedia();
            SyncDefaultMediaReceiver.completeWakefulIntent(intent);
        }
    }
}
