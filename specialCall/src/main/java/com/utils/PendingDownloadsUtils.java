package com.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.dal.objects.DAL_Access;
import com.dal.objects.IDAL;
import com.data.objects.PendingDownloadData;
import com.data.objects.SpecialMediaType;
import com.services.ServerProxyService;

import java.util.LinkedList;
import java.util.List;

import com.data.objects.PushEventKeys;
import com.exceptions.FileInvalidFormatException;
import com.files.media.MediaFile;

import static com.crashlytics.android.Crashlytics.log;
import static com.services.ServerProxyService.sendActionDownload;

/**
 * Created by Mor on 02/05/2016.
 */
public abstract class PendingDownloadsUtils {

    private static final String TAG = PendingDownloadsUtils.class.getSimpleName();

    public static void enqueuePendingDownload(final Context context, final PendingDownloadData pendingDlData) {

        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "Enqueuing pending download:" + pendingDlData);

                String sourceId = pendingDlData.getSourceId();
                String extension = pendingDlData.getMediaFile().getExtension();
                String specialMediaType = pendingDlData.getSpecialMediaType().toString();

                deletePendingDLrecordsIfNecessary(context, sourceId, extension, specialMediaType);

                ContentValues values = new ContentValues();
                values.put(IDAL.COL_SOURCE_ID, sourceId);
                values.put(IDAL.COL_DEST_ID, pendingDlData.getDestinationId());
                values.put(IDAL.COL_DEST_CONTACT, pendingDlData.getDestinationContactName());
                values.put(IDAL.COL_EXTENSION, extension);
                values.put(IDAL.COL_SOURCE_WITH_EXT, pendingDlData.getSourceId() + "." + extension);
                values.put(IDAL.COL_FILEPATH_ON_SRC_SD, pendingDlData.getFilePathOnSrcSd());
                values.put(IDAL.COL_FILETYPE, pendingDlData.getMediaFile().getFileType().toString());
                values.put(IDAL.COL_FILESIZE, pendingDlData.getMediaFile().getFileSize());
                values.put(IDAL.COL_MD5, pendingDlData.getMediaFile().getFileSize());
                values.put(IDAL.COL_COMMID, pendingDlData.getCommId());
                values.put(IDAL.COL_FILEPATH_ON_SERVER, pendingDlData.getFilePathOnServer());
                values.put(IDAL.COL_SOURCE_LOCALE, pendingDlData.getSourceLocale());
                values.put(IDAL.COL_SPECIAL_MEDIA_TYPE, specialMediaType);

                DAL_Access.getInstance(context).insertValues(IDAL.TABLE_DOWNLOADS, values);
            }
        }.start();

        //Log.d(TAG, "Download queue:" + _downloadQueue);
    }

    public static void handlePendingDownloads(final Context context) {

        new Thread() {
            @Override
            public void run() {
                log(Log.INFO,TAG, "Handling pending downloads");
                Cursor cursor = null;
                try {
                    cursor = DAL_Access.getInstance(context).getAllValues(IDAL.TABLE_DOWNLOADS);

                    PendingDownloadData pendingDownloadData = new PendingDownloadData();
                    while (cursor.moveToNext())
                    {
                        MediaFile mediaFile = new MediaFile();
                        mediaFile.setExtension(cursor.getString(cursor.getColumnIndex(IDAL.COL_EXTENSION)));
                        mediaFile.setFileType(MediaFile.FileType.valueOf(cursor.getString(cursor.getColumnIndex(IDAL.COL_FILETYPE))));
                        mediaFile.setSize(cursor.getLong(cursor.getColumnIndex(IDAL.COL_FILESIZE)));
                        mediaFile.setMd5(cursor.getString(cursor.getColumnIndex(IDAL.COL_MD5)));

                        pendingDownloadData.setSourceId(cursor.getString(cursor.getColumnIndex(IDAL.COL_SOURCE_ID)));
                        pendingDownloadData.setDestinationId(cursor.getString(cursor.getColumnIndex(IDAL.COL_DEST_ID)));
                        pendingDownloadData.setDestinationContactName(cursor.getString(cursor.getColumnIndex(IDAL.COL_DEST_CONTACT)));
                        pendingDownloadData.setFilePathOnSrcSd(cursor.getString(cursor.getColumnIndex(IDAL.COL_FILEPATH_ON_SRC_SD)));
                        pendingDownloadData.setCommId(cursor.getInt(cursor.getColumnIndex(IDAL.COL_COMMID)));
                        pendingDownloadData.setFilePathOnServer(cursor.getString(cursor.getColumnIndex(IDAL.COL_FILEPATH_ON_SERVER)));
                        pendingDownloadData.setSourceLocale(cursor.getString(cursor.getColumnIndex(IDAL.COL_SOURCE_LOCALE)));
                        pendingDownloadData.setSpecialMediaType(SpecialMediaType.valueOf(cursor.getString(cursor.getColumnIndex(IDAL.COL_SPECIAL_MEDIA_TYPE))));

                        pendingDownloadData.setMediaFile(mediaFile);

                        log(Log.INFO,TAG, "Sending pending download:" + pendingDownloadData);

                        sendActionDownload(context, pendingDownloadData);

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
            MediaFile.FileType newDownloadedFileType = MediaFilesUtils.getFileTypeByExtension(newDownloadedExtension);
            switch (newDownloadedFileType) {
                case AUDIO:

                    for (String extension : extensions) {
                        MediaFile.FileType fileType = MediaFilesUtils.getFileTypeByExtension(extension);

                        if ((fileType == MediaFile.FileType.VIDEO ||
                                        fileType == MediaFile.FileType.AUDIO)) {

                            whereCols = new String[] { IDAL.COL_SOURCE_ID, IDAL.COL_EXTENSION, IDAL.COL_SPECIAL_MEDIA_TYPE };
                            operators = new String[] { IDAL.AND, IDAL.AND };
                            whereVals = new String[] { sourceId, extension, specialMediaType };

                            DAL_Access.getInstance(context).deleteRow(IDAL.TABLE_DOWNLOADS, whereCols, operators, whereVals);
                        }
                    }
                    break;
                case IMAGE:

                    for (String extension : extensions) {
                        MediaFile.FileType fileType = MediaFilesUtils.getFileTypeByExtension(extension);

                        if ((fileType == MediaFile.FileType.VIDEO ||
                                        fileType == MediaFile.FileType.IMAGE)) {

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
