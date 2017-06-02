package com.receivers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.app.AppStateManager;
import com.services.SyncOnDefaultMediaIntentService;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by rony on 01/03/2016.
 */

public class SyncDefaultMediaReceiver extends WakefulBroadcastReceiver {

    public static final String SYNC_ACTION = "com.android.mediacallz.SYNC_DEFULAT_MEDIA_ACTION";

    //public static final int SYNC_REPEAT_INTERVAL = 3600 * 1000;

    public static final int SYNC_REPEAT_INTERVAL = 10 * 1000;

    private static final String TAG = SyncDefaultMediaReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        log(Log.DEBUG, TAG, "onReceive ACTION INTENT:" + action);

        if (AppStateManager.isLoggedIn(context)) {
            Intent serviceIntent = new Intent(context, SyncOnDefaultMediaIntentService.class);
            startWakefulService(context, serviceIntent);
        }
    }



}