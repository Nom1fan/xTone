package com_international.handlers.background_broadcast_receiver;

import android.content.Context;
import android.util.Log;

import com_international.app.AppStateManager;
import com_international.data.objects.Constants;
import com_international.handlers.Handler;
import com_international.utils.BroadcastUtils;
import com_international.utils.MediaFileUtils;
import com_international.utils.SharedPrefUtils;

import java.io.File;
import java.io.IOException;

import com_international.event.EventReport;
import com_international.event.EventType;
import com_international.utils.UtilityFactory;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 16/07/2016.
 */
public class EventUnregisterSuccessHandler implements Handler {

    private static final String TAG = EventUnregisterSuccessHandler.class.getSimpleName();

    private MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);


    @Override
    public void handle(Context ctx, Object... params) {

        try {
            //TODO Decide if we should delete MEDIA_CALLZ_HISTORY folder contents too or not
            mediaFileUtils.deleteDirectoryContents(new File(Constants.INCOMING_FOLDER));
            mediaFileUtils.deleteDirectoryContents(new File(Constants.OUTGOING_FOLDER));

            //TODO Make sure this doesn't create issues since it delete all app states and such
            SharedPrefUtils.removeAll(ctx);

            AppStateManager.setIsLoggedIn(ctx, false);

            BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.REFRESH_UI));

        } catch (IOException e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Failed during unregister procedure. [Exception]:"
                    + (e.getMessage() != null ? e.getMessage() : e));
        }

    }
}
