package com.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.dal_objects.DAL_Access;
import com.dal_objects.IDAL;
import com.services.StorageServerProxyService;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import DataObjects.DataKeys;
import DataObjects.PushEventKeys;
import Exceptions.FileInvalidFormatException;
import FilesManager.FileManager;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 02/05/2016.
 */
public abstract class DownloadsUtils {

    private static final String TAG = DownloadsUtils.class.getSimpleName();

    public static void enqueuePendingDownload(final Context context, final HashMap pendingDlData) {

        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "Enqueuing pending download:" + pendingDlData);

                String sourceId = pendingDlData.get(DataKeys.SOURCE_ID).toString();
                String extension = pendingDlData.get(DataKeys.EXTENSION).toString();
                String specialMediaType = pendingDlData.get(DataKeys.SPECIAL_MEDIA_TYPE).toString();

                deletePendingDLrecordsIfNecessary(context, sourceId, extension, specialMediaType);

                ContentValues values = new ContentValues();
                values.put(IDAL.COL_SOURCE_ID, sourceId);
                values.put(IDAL.COL_DEST_ID, pendingDlData.get(DataKeys.DESTINATION_ID).toString());
                values.put(IDAL.COL_DEST_CONTACT, pendingDlData.get(DataKeys.DESTINATION_CONTACT_NAME).toString());
                values.put(IDAL.COL_EXTENSION, extension);
                values.put(IDAL.COL_SOURCE_WITH_EXT, pendingDlData.get(DataKeys.SOURCE_WITH_EXTENSION).toString());
                values.put(IDAL.COL_FILEPATH_ON_SRC_SD, pendingDlData.get(DataKeys.FILE_PATH_ON_SRC_SD).toString());
                values.put(IDAL.COL_FILETYPE, pendingDlData.get(DataKeys.FILE_TYPE).toString());
                values.put(IDAL.COL_FILESIZE, pendingDlData.get(DataKeys.FILE_SIZE).toString());
                values.put(IDAL.COL_MD5, pendingDlData.get(DataKeys.MD5).toString());
                values.put(IDAL.COL_COMMID, pendingDlData.get(DataKeys.COMM_ID).toString());
                values.put(IDAL.COL_FILEPATH_ON_SERVER, pendingDlData.get(DataKeys.FILE_PATH_ON_SERVER).toString());
                values.put(IDAL.COL_SOURCE_LOCALE, pendingDlData.get(DataKeys.SOURCE_LOCALE).toString());
                values.put(IDAL.COL_SPECIAL_MEDIA_TYPE, pendingDlData.get(DataKeys.SPECIAL_MEDIA_TYPE).toString());

                DAL_Access.getInstance(context).insertValues(IDAL.TABLE_DOWNLOADS, values);
            }
        }.start();

        //Log.d(TAG, "Download queue:" + _downloadQueue);
    }

    public static void sendActionDownload(Context context, HashMap transferDetails) {

        Intent i = new Intent(context, StorageServerProxyService.class);
        i.setAction(StorageServerProxyService.ACTION_DOWNLOAD);
        i.putExtra(PushEventKeys.PUSH_DATA, transferDetails);
        context.startService(i);
    }

    public static void handlePendingDownloads(final Context context) {

        new Thread() {
            @Override
            public void run() {
                log(Log.INFO,TAG, "Handling pending downloads");
                Cursor cursor = null;
                try {
                    cursor = DAL_Access.getInstance(context).getAllValues(IDAL.TABLE_DOWNLOADS);

                    HashMap<DataKeys, Object> pendingDownload = new HashMap();
                    while (cursor.moveToNext())
                    {
                        pendingDownload.put(DataKeys.SOURCE_ID, cursor.getString(cursor.getColumnIndex(IDAL.COL_SOURCE_ID)));
                        pendingDownload.put(DataKeys.DESTINATION_ID, cursor.getString(cursor.getColumnIndex(IDAL.COL_DEST_ID)));
                        pendingDownload.put(DataKeys.DESTINATION_CONTACT_NAME, cursor.getString(cursor.getColumnIndex(IDAL.COL_DEST_CONTACT)));
                        pendingDownload.put(DataKeys.EXTENSION, cursor.getString(cursor.getColumnIndex(IDAL.COL_EXTENSION)));
                        pendingDownload.put(DataKeys.SOURCE_WITH_EXTENSION, cursor.getString(cursor.getColumnIndex(IDAL.COL_SOURCE_WITH_EXT)));
                        pendingDownload.put(DataKeys.FILE_PATH_ON_SRC_SD, cursor.getString(cursor.getColumnIndex(IDAL.COL_FILEPATH_ON_SRC_SD)));
                        pendingDownload.put(DataKeys.FILE_TYPE, cursor.getString(cursor.getColumnIndex(IDAL.COL_FILETYPE)));
                        pendingDownload.put(DataKeys.FILE_SIZE, cursor.getLong(cursor.getColumnIndex(IDAL.COL_FILESIZE)));
                        pendingDownload.put(DataKeys.MD5, cursor.getString(cursor.getColumnIndex(IDAL.COL_MD5)));
                        pendingDownload.put(DataKeys.COMM_ID, cursor.getInt(cursor.getColumnIndex(IDAL.COL_COMMID)));
                        pendingDownload.put(DataKeys.FILE_PATH_ON_SERVER, cursor.getString(cursor.getColumnIndex(IDAL.COL_FILEPATH_ON_SERVER)));
                        pendingDownload.put(DataKeys.SOURCE_LOCALE, cursor.getString(cursor.getColumnIndex(IDAL.COL_SOURCE_LOCALE)));
                        pendingDownload.put(DataKeys.SPECIAL_MEDIA_TYPE, cursor.getString(cursor.getColumnIndex(IDAL.COL_SPECIAL_MEDIA_TYPE)));

                        log(Log.INFO,TAG, "Sending pending download:" + pendingDownload);

                        sendActionDownload(context, pendingDownload);

                        DAL_Access.getInstance(context).deleteRow(
                                IDAL.TABLE_DOWNLOADS,
                                IDAL.COL_DOWNLOAD_ID,
                                ((Integer) cursor.getInt(cursor.getColumnIndex(IDAL.COL_DOWNLOAD_ID))).toString());
                    }
                }
                finally {
                    if(cursor!=null)
                        cursor.close();
                }
            }
        }.start();

    }

    /**
     * Deletes records containing the source's designated DB row by an algorithm based on the new downloaded file type:
     * This method does not delete the new downloaded file.
     * lets mark newDownloadedFileType as nDFT.
     * nDFT = IMAGE --> deletes images and videos
     * nDFT = AUDIO --> deletes audios and videos
     * nDFT = VIDEO --> deletes all
     *
     * @param newDownloadedExtension The extension of the pending download file just arrived and should be created in the db
     * @param sourceId                The sourceId number of the sender of the file
     */
    private static void deletePendingDLrecordsIfNecessary(Context context, String sourceId, String newDownloadedExtension, String specialMediaType) {

        String[] whereCols = new String[] { IDAL.COL_SOURCE_ID, IDAL.COL_SPECIAL_MEDIA_TYPE };
        String[] operators = new String[] { IDAL.AND };
        String[] whereVals = new String[] { sourceId, specialMediaType };

        Cursor pendingDlsFromSrc = DAL_Access.getInstance(context).getValues(IDAL.TABLE_DOWNLOADS, whereCols, operators, whereVals);

        List<String> extensions = new LinkedList<>();
        while(pendingDlsFromSrc.moveToNext()) {
            String extension = pendingDlsFromSrc.getString(pendingDlsFromSrc.getColumnIndex(IDAL.COL_EXTENSION));
            extensions.add(extension);
        }

        try {
            FileManager.FileType newDownloadedFileType = FileManager.getFileTypeByExtension(newDownloadedExtension);
            switch (newDownloadedFileType) {
                case AUDIO:

                    for (String extension : extensions) {
                        FileManager.FileType fileType = FileManager.getFileTypeByExtension(extension);

                        if ((fileType == FileManager.FileType.VIDEO ||
                                        fileType == FileManager.FileType.AUDIO)) {

                            whereCols = new String[] { IDAL.COL_SOURCE_ID, IDAL.COL_EXTENSION, IDAL.COL_SPECIAL_MEDIA_TYPE };
                            operators = new String[] { IDAL.AND, IDAL.AND };
                            whereVals = new String[] { sourceId, extension, specialMediaType };

                            DAL_Access.getInstance(context).deleteRow(IDAL.TABLE_DOWNLOADS, whereCols, operators, whereVals);
                        }
                    }
                    break;
                case IMAGE:

                    for (String extension : extensions) {
                        FileManager.FileType fileType = FileManager.getFileTypeByExtension(extension);

                        if ((fileType == FileManager.FileType.VIDEO ||
                                        fileType == FileManager.FileType.IMAGE)) {

                            whereCols = new String[] { IDAL.COL_SOURCE_ID, IDAL.COL_EXTENSION, IDAL.COL_SPECIAL_MEDIA_TYPE };
                            operators = new String[] { IDAL.AND, IDAL.AND };
                            whereVals = new String[] { sourceId, extension, specialMediaType };

                            DAL_Access.getInstance(context).deleteRow(IDAL.TABLE_DOWNLOADS, whereCols, operators, whereVals);
                        }
                    }
                    break;

                case VIDEO:

                    for (String extension : extensions) {

                        whereCols = new String[] { IDAL.COL_SOURCE_ID, IDAL.COL_EXTENSION, IDAL.COL_SPECIAL_MEDIA_TYPE };
                        operators = new String[] { IDAL.AND, IDAL.AND };
                        whereVals = new String[] { sourceId, extension, specialMediaType };

                        DAL_Access.getInstance(context).deleteRow(IDAL.TABLE_DOWNLOADS, whereCols, operators, whereVals);
                    }
                    break;
            }

        } catch (FileInvalidFormatException e) {
            e.printStackTrace();
        }

    }
}
