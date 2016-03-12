package com.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.services.LogicServerProxyService;
import com.services.StorageServerProxyService;

import java.io.File;

import DataObjects.CallRecord;
import DataObjects.SpecialMediaType;
import DataObjects.TransferDetails;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;

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
                        visualExists ? new FileManager(visualMediaPath) : null,
                        SharedPrefUtils.getString(context, SharedPrefUtils.SERVICES, SharedPrefUtils.TEMP_VISUALMD5),
                        audioExists ? new FileManager(audioMediaPath) : null,
                        SharedPrefUtils.getString(context, SharedPrefUtils.SERVICES, SharedPrefUtils.TEMP_AUDIOMD5),
                        specialMediaType);

                Log.i(TAG, "Reporting MC:" + callRecord);

                Intent i = new Intent(context, LogicServerProxyService.class);
                i.setAction(LogicServerProxyService.ACTION_INSERT_CALL_RECORD);
                i.putExtra(LogicServerProxyService.CALL_RECORD, callRecord);
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
