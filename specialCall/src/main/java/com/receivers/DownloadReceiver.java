package com.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;

import DataObjects.TransferDetails;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;
import com.data_objects.Constants;
import com.utils.SharedPrefUtils;

public class DownloadReceiver extends BroadcastReceiver {

    private static final String TAG = DownloadReceiver.class.getSimpleName();
    private Context _context;
    /**
     * Listener for downloads
     * Responsible for setting/deleting files and preparing for later media display after a new successful download event is received
     */

        @Override
        public void onReceive(Context context, Intent intent) {
            EventReport eventReport = (EventReport) intent.getSerializableExtra(Event.EVENT_REPORT);
            _context = context;
            if (eventReport.status() == EventType.DOWNLOAD_SUCCESS) {
                Log.i(TAG, "In: DOWNLOAD_SUCCESS");
                TransferDetails td = (TransferDetails) eventReport.data();
                FileManager.FileType fType = td.getFileType();
                String extension = td.getExtension();
                String fFullName = td.getSourceWithExtension();
                String source = td.getSourceId();
                String fileFullPath = Constants.specialCallIncomingPath+source+"/"+fFullName;

                switch (fType) {
                    case RINGTONE:
                        setNewRingTone(source, fileFullPath);
                        deleteFilesIfNecessary(fFullName, fType, source);
                        break;

                    case VIDEO:
                    case IMAGE:
                        SharedPrefUtils.setString(_context,
                                SharedPrefUtils.MEDIA_FILEPATH, source, fileFullPath);
                        deleteFilesIfNecessary(fFullName, fType, source);
                        break;
                }


            }

        }

    /**
     * Deletes files in the source's designated directory by an algorithm based on the new downloaded file type:
     * This method does not delete the new downloaded file.
     * lets mark newDownloadedFileType as nDFT.
     * nDFT = IMAGE --> deletes images and videos
     * nDFT = RINGTONE --> deletes ringtones and videos
     * nDFT = VIDEO --> deletes all
     *
     * @param newDownloadedFileType The type of the files just downloaded and should be created in the source designated folder
     * @param source The source number of the sender of the file
     */
    private void deleteFilesIfNecessary(String addedFileName, FileManager.FileType newDownloadedFileType, String source) {

        File spDir = new File(Constants.specialCallIncomingPath+source);
        File[] files = spDir.listFiles();
        try
        {
            switch (newDownloadedFileType)
            {
                case RINGTONE:

                    for (int i = 0; i < files.length; ++i)
                    {
                        File file = files[i];
                        String fileName = file.getName(); // This includes extension
                        FileManager.FileType fileType = FileManager.getFileType(file);

                        if (!fileName.equals(addedFileName) &&
                                (fileType == FileManager.FileType.VIDEO ||
                                        fileType == FileManager.FileType.RINGTONE)) {
                            FileManager.delete(file);
                            SharedPrefUtils.remove(_context, SharedPrefUtils.MEDIA_FILEPATH, source);
                        }
                    }
                    break;
                case IMAGE:

                    for (int i = 0; i < files.length; ++i)
                    {
                        File file = files[i];
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

                    for (int i = 0; i < files.length; ++i)
                    {
                        File file = files[i];
                        String fileName = file.getName(); // This includes extension
                        if(!fileName.equals(addedFileName)) {
                            FileManager.delete(file);
                            SharedPrefUtils.remove(_context, SharedPrefUtils.RINGTONE_FILEPATH, source);
                        }
                    }
                    break;
            }

        }
        catch (FileInvalidFormatException e) {
            e.printStackTrace();
            Log.e(TAG, "Invalid file type:"+e.getMessage()+" in SpecialCall directory of source:"+source);
        } catch (FileDoesNotExistException | FileMissingExtensionException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

    }

    private void setNewRingTone(String source ,String ringToneFilePath) {

        Log.i(TAG, "setNewRingTone with sharedPrefs: " + ringToneFilePath);
            SharedPrefUtils.setString(_context,
                    SharedPrefUtils.RINGTONE_FILEPATH, source,
                    ringToneFilePath);
    }

}
