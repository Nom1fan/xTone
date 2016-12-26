package com.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;

import com.data.objects.CallRecord;
import com.data.objects.SpecialMediaType;
import com.exceptions.FileDoesNotExistException;
import com.exceptions.FileExceedsMaxSizeException;
import com.exceptions.FileInvalidFormatException;
import com.exceptions.FileMissingExtensionException;
import com.files.media.MediaFile;
import com.services.ServerProxyService;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 08/03/2016.
 */
public abstract class MCHistoryUtils {

    private static final String TAG = MCHistoryUtils.class.getSimpleName();

    public static void reportMC(Context context, String src, String dest, String visualMediaPath, String audioMediaPath ,SpecialMediaType specialMediaType) {

        boolean visualExists = false;
        boolean audioExists = false;

        if(visualMediaPath!=null) {
            File visualFile = new File(visualMediaPath);
            if(visualFile.exists())
                visualExists = true;
        }

        if(audioMediaPath!=null) {
            File audioFile = new File(audioMediaPath);
            if(audioFile.exists())
                audioExists = true;
        }

        try {
            if(visualExists || audioExists) {

                CallRecord callRecord = new CallRecord(
                        src,
                        dest,
                        visualExists ? new MediaFile(visualMediaPath) : null,
                        SharedPrefUtils.getString(context, SharedPrefUtils.SERVICES, SharedPrefUtils.TEMP_VISUALMD5),
                        audioExists ? new MediaFile(audioMediaPath) : null,
                        SharedPrefUtils.getString(context, SharedPrefUtils.SERVICES, SharedPrefUtils.TEMP_AUDIOMD5),
                        specialMediaType);

                log(Log.INFO,TAG, "Reporting MC:" + callRecord);

                Intent i = new Intent(context, ServerProxyService.class);
                i.setAction(ServerProxyService.ACTION_INSERT_CALL_RECORD);
                i.putExtra(ServerProxyService.CALL_RECORD, callRecord);
                context.startService(i);
            }

        } catch (FileInvalidFormatException  |
                 FileExceedsMaxSizeException |
                 FileDoesNotExistException   |
                 FileMissingExtensionException e) {
            e.printStackTrace();
        }
    }
}
