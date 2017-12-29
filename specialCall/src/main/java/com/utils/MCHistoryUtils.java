package com.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.data.objects.MediaCall;
import com.enums.SpecialMediaType;
import com.exceptions.FileException;
import com.files.media.MediaFile;
import com.services.ServerProxyService;

import java.io.File;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 08/03/2016.
 */
public abstract class MCHistoryUtils {

    private static final String TAG = MCHistoryUtils.class.getSimpleName();

    public static void reportMC(Context context, String src, String dest, String visualMediaPath, String audioMediaPath, SpecialMediaType specialMediaType) {

        MediaFile visualMediaFile = null;
        MediaFile audioMediaFile = null;

        if (visualMediaPath != null) {
            visualMediaFile = prepareMediaFile(context, visualMediaPath, SharedPrefUtils.TEMP_VISUALMD5);
        }
        if (audioMediaPath != null) {
            audioMediaFile = prepareMediaFile(context, audioMediaPath, SharedPrefUtils.TEMP_AUDIOMD5);
        }

        if (visualMediaFile != null || audioMediaFile != null) {

            MediaCall mediaCall = new MediaCall(src, dest, visualMediaFile, audioMediaFile, specialMediaType);

            log(Log.INFO, TAG, "Reporting MC:" + mediaCall);
            Intent i = new Intent(context, ServerProxyService.class);
            i.setAction(ServerProxyService.ACTION_INSERT_CALL_RECORD);
            i.putExtra(ServerProxyService.MEDIA_CALL, mediaCall);
            context.startService(i);
        }
    }

    private static MediaFile prepareMediaFile(Context context, String mediaPath, String sharedPrefsTempMd5Key) {
        String md5 = SharedPrefUtils.getString(context, SharedPrefUtils.SERVICES, sharedPrefsTempMd5Key);
        MediaFile mediaFile = new MediaFile(new File(mediaPath));
        mediaFile.setMd5(md5);
        return mediaFile;
    }
}
