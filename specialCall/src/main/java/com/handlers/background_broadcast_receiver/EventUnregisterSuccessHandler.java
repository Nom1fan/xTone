package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.util.Log;

import com.app.AppStateManager;
import com.data_objects.Constants;
import com.handlers.Handler;
import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;

import java.io.File;
import java.io.IOException;

import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventUnregisterSuccessHandler implements Handler {

    private static final String TAG = EventUnregisterSuccessHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {

        try {
            //TODO Decide if we should delete MEDIA_CALLZ_HISTORY folder contents too or not
            FileManager.deleteDirectoryContents(new File(Constants.INCOMING_FOLDER));
            FileManager.deleteDirectoryContents(new File(Constants.OUTGOING_FOLDER));

            //TODO Make sure this doesn't create issues since it delete all app states and such
            SharedPrefUtils.removeAll(ctx);

            AppStateManager.setIsLoggedIn(ctx, false);

            BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.REFRESH_UI));

        } catch (IOException e) {
            e.printStackTrace();
            log(Log.ERROR,TAG, "Failed during unregister procedure. [Exception]:"
                    + (e.getMessage() != null ? e.getMessage() : e));
        }

    }
}
