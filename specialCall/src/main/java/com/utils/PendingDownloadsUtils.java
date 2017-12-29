package com.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.dao.DAOFactory;
import com.dao.SQLiteDAO;
import com.data.objects.PendingDownloadData;
import com.enums.SpecialMediaType;

import java.util.LinkedList;
import java.util.List;

import com.files.media.MediaFile;

import static com.crashlytics.android.Crashlytics.log;
import static com.services.ServerProxyService.sendActionDownload;

/**
 * Created by Mor on 02/05/2016.
 */
public abstract class PendingDownloadsUtils {

    private static final String TAG = PendingDownloadsUtils.class.getSimpleName();

    private static MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);

    public static void enqueuePendingDownload(final Context context, final PendingDownloadData pendingDlData) {
        final SQLiteDAO dao = DAOFactory.getSQLiteDAO(context);

        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "Enqueuing pending download:" + pendingDlData);

                String sourceId = pendingDlData.getSourceId();
                String extension = pendingDlData.getMediaFile().getExtension();
                String specialMediaType = pendingDlData.getSpecialMediaType().toString();

                deletePendingDLrecordsIfNecessary(context, sourceId, extension, specialMediaType);

                ContentValues values = new ContentValues();
                values.put(SQLiteDAO.COL_SOURCE_ID, sourceId);
                values.put(SQLiteDAO.COL_DEST_ID, pendingDlData.getDestinationId());
                values.put(SQLiteDAO.COL_DEST_CONTACT, pendingDlData.getDestinationContactName());
                values.put(SQLiteDAO.COL_EXTENSION, extension);
                values.put(SQLiteDAO.COL_SOURCE_WITH_EXT, pendingDlData.getSourceId() + "." + extension);
                values.put(SQLiteDAO.COL_FILEPATH_ON_SRC_SD, pendingDlData.getFilePathOnSrcSd());
                values.put(SQLiteDAO.COL_FILETYPE, pendingDlData.getMediaFile().getFileType().toString());
                values.put(SQLiteDAO.COL_FILESIZE, pendingDlData.getMediaFile().getSize());
                values.put(SQLiteDAO.COL_MD5, pendingDlData.getMediaFile().getSize());
                values.put(SQLiteDAO.COL_COMMID, pendingDlData.getCommId());
                values.put(SQLiteDAO.COL_FILEPATH_ON_SERVER, pendingDlData.getFilePathOnServer());
                values.put(SQLiteDAO.COL_SOURCE_LOCALE, pendingDlData.getSourceLocale());
                values.put(SQLiteDAO.COL_SPECIAL_MEDIA_TYPE, specialMediaType);

                dao.insertValues(SQLiteDAO.TABLE_DOWNLOADS, values);
            }
        }.start();

        //Log.d(TAG, "Download queue:" + _downloadQueue);
    }

    public static void handlePendingDownloads(final Context context) {
        final SQLiteDAO dao = DAOFactory.getSQLiteDAO(context);

        new Thread() {
            @Override
            public void run() {
                log(Log.INFO, TAG, "Handling pending downloads");
                Cursor cursor = null;
                try {
                    cursor = dao.getAllValues(SQLiteDAO.TABLE_DOWNLOADS);

                    PendingDownloadData pendingDownloadData = new PendingDownloadData();
                    while (cursor.moveToNext()) {
                        MediaFile mediaFile = new MediaFile();
                        mediaFile.setExtension(cursor.getString(cursor.getColumnIndex(SQLiteDAO.COL_EXTENSION)));
                        mediaFile.setFileType(MediaFile.FileType.valueOf(cursor.getString(cursor.getColumnIndex(SQLiteDAO.COL_FILETYPE))));
                        mediaFile.setSize(cursor.getLong(cursor.getColumnIndex(SQLiteDAO.COL_FILESIZE)));
                        mediaFile.setMd5(cursor.getString(cursor.getColumnIndex(SQLiteDAO.COL_MD5)));

                        pendingDownloadData.setSourceId(cursor.getString(cursor.getColumnIndex(SQLiteDAO.COL_SOURCE_ID)));
                        pendingDownloadData.setDestinationId(cursor.getString(cursor.getColumnIndex(SQLiteDAO.COL_DEST_ID)));
                        pendingDownloadData.setDestinationContactName(cursor.getString(cursor.getColumnIndex(SQLiteDAO.COL_DEST_CONTACT)));
                        pendingDownloadData.setFilePathOnSrcSd(cursor.getString(cursor.getColumnIndex(SQLiteDAO.COL_FILEPATH_ON_SRC_SD)));
                        pendingDownloadData.setCommId(cursor.getInt(cursor.getColumnIndex(SQLiteDAO.COL_COMMID)));
                        pendingDownloadData.setFilePathOnServer(cursor.getString(cursor.getColumnIndex(SQLiteDAO.COL_FILEPATH_ON_SERVER)));
                        pendingDownloadData.setSourceLocale(cursor.getString(cursor.getColumnIndex(SQLiteDAO.COL_SOURCE_LOCALE)));
                        pendingDownloadData.setSpecialMediaType(SpecialMediaType.valueOf(cursor.getString(cursor.getColumnIndex(SQLiteDAO.COL_SPECIAL_MEDIA_TYPE))));

                        pendingDownloadData.setMediaFile(mediaFile);

                        log(Log.INFO, TAG, "Sending pending download:" + pendingDownloadData);

                        sendActionDownload(context, pendingDownloadData);

                        dao.deleteRow(
                                SQLiteDAO.TABLE_DOWNLOADS,
                                SQLiteDAO.COL_DOWNLOAD_ID,
                                ((Integer) cursor.getInt(cursor.getColumnIndex(SQLiteDAO.COL_DOWNLOAD_ID))).toString());
                    }
                } finally {
                    if (cursor != null)
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
     * @param sourceId               The sourceId number of the sender of the file
     */
    private static void deletePendingDLrecordsIfNecessary(Context context, String sourceId, String newDownloadedExtension, String specialMediaType) {
        SQLiteDAO dao = DAOFactory.getSQLiteDAO(context);


        String[] whereCols = new String[]{SQLiteDAO.COL_SOURCE_ID, SQLiteDAO.COL_SPECIAL_MEDIA_TYPE};
        String[] operators = new String[]{SQLiteDAO.AND};
        String[] whereVals = new String[]{sourceId, specialMediaType};

        Cursor pendingDlsFromSrc = dao.getValues(SQLiteDAO.TABLE_DOWNLOADS, whereCols, operators, whereVals);

        List<String> extensions = new LinkedList<>();
        while (pendingDlsFromSrc.moveToNext()) {
            String extension = pendingDlsFromSrc.getString(pendingDlsFromSrc.getColumnIndex(SQLiteDAO.COL_EXTENSION));
            extensions.add(extension);
        }


        MediaFile.FileType newDownloadedFileType = mediaFileUtils.getFileTypeByExtension(newDownloadedExtension);
        if (newDownloadedFileType != null){
            switch (newDownloadedFileType) {
                case AUDIO:

                    for (String extension : extensions) {
                        MediaFile.FileType fileType = mediaFileUtils.getFileTypeByExtension(extension);

                        if ((fileType == MediaFile.FileType.VIDEO ||
                                fileType == MediaFile.FileType.AUDIO)) {

                            whereCols = new String[]{SQLiteDAO.COL_SOURCE_ID, SQLiteDAO.COL_EXTENSION, SQLiteDAO.COL_SPECIAL_MEDIA_TYPE};
                            operators = new String[]{SQLiteDAO.AND, SQLiteDAO.AND};
                            whereVals = new String[]{sourceId, extension, specialMediaType};

                            dao.deleteRow(SQLiteDAO.TABLE_DOWNLOADS, whereCols, operators, whereVals);
                        }
                    }
                    break;
                case IMAGE:

                    for (String extension : extensions) {
                        MediaFile.FileType fileType = mediaFileUtils.getFileTypeByExtension(extension);

                        if ((fileType == MediaFile.FileType.VIDEO ||
                                fileType == MediaFile.FileType.IMAGE)) {

                            whereCols = new String[]{SQLiteDAO.COL_SOURCE_ID, SQLiteDAO.COL_EXTENSION, SQLiteDAO.COL_SPECIAL_MEDIA_TYPE};
                            operators = new String[]{SQLiteDAO.AND, SQLiteDAO.AND};
                            whereVals = new String[]{sourceId, extension, specialMediaType};

                            dao.deleteRow(SQLiteDAO.TABLE_DOWNLOADS, whereCols, operators, whereVals);
                        }
                    }
                    break;

                case VIDEO:

                    for (String extension : extensions) {

                        whereCols = new String[]{SQLiteDAO.COL_SOURCE_ID, SQLiteDAO.COL_EXTENSION, SQLiteDAO.COL_SPECIAL_MEDIA_TYPE};
                        operators = new String[]{SQLiteDAO.AND, SQLiteDAO.AND};
                        whereVals = new String[]{sourceId, extension, specialMediaType};

                        dao.deleteRow(SQLiteDAO.TABLE_DOWNLOADS, whereCols, operators, whereVals);
                    }
                    break;
            }
        }

    }
}
