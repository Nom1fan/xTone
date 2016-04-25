package com.receivers;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.data_objects.Constants;
import com.utils.SharedPrefUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.HashMap;

import DataObjects.DataKeys;
import DataObjects.SharedConstants;
import DataObjects.SpecialMediaType;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;


public class DownloadReceiver extends BroadcastReceiver {

    private static final String TAG = DownloadReceiver.class.getSimpleName();
    private String _newFileFullPath;
    private String _newFileDir;
    private String _sharedPrefKeyForVisualMedia;
    private String _sharedPrefKeyForAudioMedia;


    /**
     * Listener for downloads
     * Responsible for setting/deleting files and preparing for later media display after a new successful download event is received
     */

    @Override
    public void onReceive(Context context, Intent intent) {
        EventReport eventReport = (EventReport) intent.getSerializableExtra(Event.EVENT_REPORT);

        if (eventReport.status() == EventType.DOWNLOAD_SUCCESS) {
            Log.i(TAG, "In: DOWNLOAD_SUCCESS");

            HashMap td = (HashMap) eventReport.data();
            preparePathsAndDirs(td);

            // copy new downloaded file to History Folder so it will show up in Gallery and don't make any duplicates with MD5 signature
            if(SharedPrefUtils.getBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.ALWAYS_SAVE_MEDIA))
                 copyToHistoryForGalleryShow(context, td);

            FileManager.FileType fType = FileManager.FileType.valueOf(td.get(DataKeys.FILE_TYPE).toString());
            String fFullName = td.get(DataKeys.SOURCE_WITH_EXTENSION).toString();
            String source = td.get(DataKeys.SOURCE_ID).toString();
            String md5 = td.get(DataKeys.MD5).toString();

            switch (fType) {
                case AUDIO:
                    setNewRingTone(context, source, md5);
                    deleteFilesIfNecessary(context, fFullName, fType, source);
                    break;

                case VIDEO:
                case IMAGE:
                    setNewVisualMedia(context, source, md5);
                    deleteFilesIfNecessary(context, fFullName, fType, source);
                    break;
            }
        }
    }

    /**
     * Deletes files in the source's designated directory by an algorithm based on the new downloaded file type:
     * This method does not delete the new downloaded file.
     * lets mark newDownloadedFileType as nDFT.
     * nDFT = IMAGE --> deletes images and videos
     * nDFT = AUDIO --> deletes ringtones and videos
     * nDFT = VIDEO --> deletes all
     *
     * @param newDownloadedFileType The type of the files just downloaded and should be created in the source designated folder
     * @param source                The source number of the sender of the file
     */
    private void deleteFilesIfNecessary(Context context, String addedFileName, FileManager.FileType newDownloadedFileType, String source) {

        File[] files = new File(_newFileDir).listFiles();

        try {
            switch (newDownloadedFileType) {
                case AUDIO:

                    for (File file : files) {
                        String fileName = file.getName(); // This includes extension
                        FileManager.FileType fileType = FileManager.getFileType(file);

                        if (!fileName.equals(addedFileName) &&
                                (fileType == FileManager.FileType.VIDEO ||
                                        fileType == FileManager.FileType.AUDIO)) {
                            FileManager.delete(file);
                            SharedPrefUtils.remove(context, _sharedPrefKeyForVisualMedia, source);
                        }
                    }
                    break;
                case IMAGE:

                    for (File file : files) {
                        String fileName = file.getName(); // This includes extension
                        FileManager.FileType fileType = FileManager.getFileType(file);

                        if (!fileName.equals(addedFileName) &&
                                (fileType == FileManager.FileType.VIDEO ||
                                        fileType == FileManager.FileType.IMAGE)) {
                            FileManager.delete(file);
                        }
                    }
                    break;

                case VIDEO:

                    for (File file : files) {
                        String fileName = file.getName(); // This includes extension
                        if (!fileName.equals(addedFileName)) {
                            FileManager.delete(file);
                            SharedPrefUtils.remove(context, _sharedPrefKeyForAudioMedia, source);
                        }
                    }
                    break;
            }

        } catch (FileInvalidFormatException e) {
            e.printStackTrace();
            Log.e(TAG, "Invalid file type:" + e.getMessage() + " in SpecialCall directory of source:" + source);
        } catch (FileDoesNotExistException | FileMissingExtensionException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

    }

    private void setNewRingTone(Context context, String source, String md5) {

        Log.i(TAG, "setNewRingTone with sharedPrefs: " + _newFileFullPath);
        SharedPrefUtils.setString(context,
                _sharedPrefKeyForAudioMedia, source, _newFileFullPath);

        // Backing up audio file md5
        SharedPrefUtils.setString(context,
                _sharedPrefKeyForAudioMedia, _newFileFullPath, md5);
    }

    private void setNewVisualMedia(Context context, String source, String md5) {

        Log.i(TAG, "setNewVisualMedia with sharedPrefs: " + _newFileFullPath);
        SharedPrefUtils.setString(context,
                _sharedPrefKeyForVisualMedia, source, _newFileFullPath);

        // Backing up visual file md5
        SharedPrefUtils.setString(context, _sharedPrefKeyForAudioMedia, _newFileFullPath, md5);
    }

    private void preparePathsAndDirs(HashMap td) {

        String specialMediaType = td.get(DataKeys.SPECIAL_MEDIA_TYPE).toString();
        String srcId = td.get(DataKeys.SOURCE_ID).toString();
        String srcWithExtension = td.get(DataKeys.SOURCE_WITH_EXTENSION).toString();

        // Preparing for appropriate special media type
        switch (SpecialMediaType.valueOf(specialMediaType)) {
            case CALLER_MEDIA:
                _newFileDir = Constants.INCOMING_FOLDER + srcId;
                _sharedPrefKeyForVisualMedia = SharedPrefUtils.CALLER_MEDIA_FILEPATH;
                _sharedPrefKeyForAudioMedia = SharedPrefUtils.RINGTONE_FILEPATH;
                break;
            case PROFILE_MEDIA:
                _newFileDir = Constants.OUTGOING_FOLDER + srcId;
                _sharedPrefKeyForVisualMedia = SharedPrefUtils.PROFILE_MEDIA_FILEPATH;
                _sharedPrefKeyForAudioMedia = SharedPrefUtils.FUNTONE_FILEPATH;
                break;

            default:
                throw new UnsupportedOperationException("Invalid SpecialMediaType received");
        }

        _newFileFullPath = _newFileDir + "/" + srcWithExtension;
    }

    private void copyToHistoryForGalleryShow(Context context, HashMap td) {

        try {

            File downloadedFile = new File(_newFileFullPath);
            String extension = td.get(DataKeys.EXTENSION).toString();
            String md5 = td.get(DataKeys.MD5).toString();
            FileManager.FileType fileType = FileManager.FileType.valueOf(td.get(DataKeys.FILE_TYPE).toString());

            String historyFileName = Constants.HISTORY_FOLDER + fileType + "_" + md5 + "." + extension; //give a unique name to the file and make sure there won't be any duplicates
            File copyToHistoryFile = new File(historyFileName);

            if (!copyToHistoryFile.exists()) // if the file exist don't do any duplicate
            {
                FileUtils.copyFile(downloadedFile, copyToHistoryFile);
                Log.i(TAG, "Creating a unique md5 file in the History Folder fileName:  " + copyToHistoryFile.getName());
                if (fileType == FileManager.FileType.AUDIO) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DATA, copyToHistoryFile.getName());
                    values.put(MediaStore.MediaColumns.TITLE, copyToHistoryFile.getName());
                    values.put(MediaStore.Audio.AudioColumns.ARTIST, copyToHistoryFile.getName());
                    values.put(MediaStore.Audio.AudioColumns.ARTIST_ID, copyToHistoryFile.getName());
                    values.put(MediaStore.Audio.AudioColumns.ALBUM, SharedConstants.APP_NAME);
                    values.put(MediaStore.Audio.AudioColumns.ALBUM_KEY, SharedConstants.APP_NAME);
                    values.put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, copyToHistoryFile.getName());
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
                    values.put(MediaStore.Audio.Media.IS_MUSIC, true);
                    Uri uri = MediaStore.Audio.Media.getContentUriForPath(historyFileName);
                    context.getContentResolver().insert(uri, values);

                }
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(copyToHistoryFile)));
            } else {
                Log.e(TAG, "File already exist: " + historyFileName);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
