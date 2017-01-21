package com.handlers.background_broadcast_receiver;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.data.objects.Constants;
import com.data.objects.Contact;
import com.data.objects.PendingDownloadData;
import com.data.objects.SpecialMediaType;
import com.event.EventReport;
import com.exceptions.FileDoesNotExistException;
import com.exceptions.FileInvalidFormatException;
import com.exceptions.FileMissingExtensionException;
import com.files.media.MediaFile;
import com.handlers.Handler;
import com.utils.ContactsUtils;
import com.utils.MediaFilesUtils;
import com.utils.PhoneNumberUtils;
import com.utils.SharedPrefUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Handler for downloads
 * Responsible for setting/deleting files and preparing for later media display after a new successful download event is received
 */
public class EventDownloadReceivedHandler implements Handler {

    private static final String TAG = EventDownloadReceivedHandler.class.getSimpleName();
    private String newFileFullPath;
    private String newFileDir;
    private String sharedPrefKeyForVisualMedia;
    private String sharedPrefKeyForAudioMedia;

    @Override
    public void handle(Context ctx, Object... params) {
        EventReport eventReport = (EventReport) params[0];

        Crashlytics.log(Log.INFO, TAG, "In: DOWNLOAD_SUCCESS");

        PendingDownloadData downloadData = (PendingDownloadData) eventReport.data();
        preparePathsAndDirs(downloadData);

        // Copy new downloaded file to History Folder so it will show up in Gallery and don't make any duplicates with MD5 signature
        if (isAuthorizedToLeaveMedia(ctx, downloadData.getSourceId()))
            copyToHistoryForGalleryShow(ctx, downloadData);

        MediaFile.FileType fType = downloadData.getMediaFile().getFileType();
        String fFullName = downloadData.getSourceId() + "." + downloadData.getMediaFile().getExtension();
        String source = downloadData.getSourceId();
        String md5 = downloadData.getMediaFile().getMd5();

        switch (fType) {
            case AUDIO:
                setNewRingTone(ctx, source, md5);
                deleteFilesIfNecessary(ctx, fFullName, fType, source);
                break;

            case VIDEO:
            case IMAGE:
                setNewVisualMedia(ctx, source, md5);
                deleteFilesIfNecessary(ctx, fFullName, fType, source);
                break;
        }
    }


    private boolean isAuthorizedToLeaveMedia(Context context, String incomingNumber) {


        if (SharedPrefUtils.getInt(context, SharedPrefUtils.GENERAL, SharedPrefUtils.SAVE_MEDIA_OPTION) == 0) // Save Always
            return true;
        else if (SharedPrefUtils.getInt(context, SharedPrefUtils.GENERAL, SharedPrefUtils.SAVE_MEDIA_OPTION) == 1) // Contacts Only Save
        {            // GET ALL CONTACTS
            List<Contact> contactsList = ContactsUtils.getAllContacts(context);
            List<String> contactPhonenumbers = new ArrayList<>();

            for (int i = 0; i < contactsList.size(); i++) {
                contactPhonenumbers.add(contactsList.get(i).get_phoneNumber());
            }

            if (contactPhonenumbers.contains(PhoneNumberUtils.toValidLocalPhoneNumber(incomingNumber)))
                return true; // authorized to save media
            else
                return false; // not authorized
        } else // 2 - never save
            return false;

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
    private void deleteFilesIfNecessary(Context context, String addedFileName, MediaFile.FileType newDownloadedFileType, String source) {

        File[] files = new File(newFileDir).listFiles();

        if(files == null)
            return;

        try {
            switch (newDownloadedFileType) {
                case AUDIO:

                    for (File file : files) {
                        String fileName = file.getName(); // This includes extension
                        MediaFile.FileType fileType = MediaFilesUtils.getFileType(file);

                        if (!fileName.equals(addedFileName) &&
                                (fileType == MediaFile.FileType.VIDEO ||
                                        fileType == MediaFile.FileType.AUDIO)) {
                            MediaFilesUtils.delete(file);
                            SharedPrefUtils.remove(context, sharedPrefKeyForVisualMedia, source);
                        }
                    }
                    break;
                case IMAGE:

                    for (File file : files) {
                        String fileName = file.getName(); // This includes extension
                        MediaFile.FileType fileType = MediaFilesUtils.getFileType(file);

                        if (!fileName.equals(addedFileName) &&
                                (fileType == MediaFile.FileType.VIDEO ||
                                        fileType == MediaFile.FileType.IMAGE)) {
                            MediaFilesUtils.delete(file);
                        }
                    }
                    break;

                case VIDEO:

                    for (File file : files) {
                        String fileName = file.getName(); // This includes extension
                        if (!fileName.equals(addedFileName)) {
                            MediaFilesUtils.delete(file);
                            SharedPrefUtils.remove(context, sharedPrefKeyForAudioMedia, source);
                        }
                    }
                    break;
            }

        } catch (FileInvalidFormatException e) {
            e.printStackTrace();
            Crashlytics.log(Log.ERROR, TAG, "Invalid file type:" + e.getMessage() + " in SpecialCall directory of source:" + source);
        } catch (FileDoesNotExistException | FileMissingExtensionException e) {
            e.printStackTrace();
            Crashlytics.log(Log.ERROR, TAG, e.getMessage());
        }

    }

    private void setNewRingTone(Context context, String source, String md5) {

        Crashlytics.log(Log.INFO, TAG, "setNewRingTone with sharedPrefs: " + newFileFullPath);

        if (!MediaFilesUtils.isAudioFileCorrupted(newFileFullPath, context)) {
            SharedPrefUtils.setString(context,
                    sharedPrefKeyForAudioMedia, source, newFileFullPath);

            // Backing up audio file md5
            SharedPrefUtils.setString(context,
                    sharedPrefKeyForAudioMedia, newFileFullPath, md5);
        } else {
            Crashlytics.log(Log.ERROR, TAG, "CORRUPTED FILE : setNewRingTone with sharedPrefs: " + newFileFullPath);
        }
    }

    private void setNewVisualMedia(Context context, String source, String md5) {

        Crashlytics.log(Log.INFO, TAG, "setNewVisualMedia with sharedPrefs: " + newFileFullPath);
        if (!MediaFilesUtils.isVideoFileCorrupted(newFileFullPath, context)) {
            SharedPrefUtils.setString(context,
                    sharedPrefKeyForVisualMedia, source, newFileFullPath);

            // Backing up visual file md5
            SharedPrefUtils.setString(context, sharedPrefKeyForVisualMedia, newFileFullPath, md5);
        } else {
            Crashlytics.log(Log.ERROR, TAG, "CORRUPTED FILE : setNewVisualMedia with sharedPrefs: " + newFileFullPath);
        }
    }

    private void preparePathsAndDirs(PendingDownloadData downloadData) {

        SpecialMediaType specialMediaType = downloadData.getSpecialMediaType();
        String srcId = downloadData.getSourceId();
        String srcWithExtension = downloadData.getSourceId() + "." + downloadData.getMediaFile().getExtension();

        // Preparing for appropriate special media type
        switch (specialMediaType) {
            case CALLER_MEDIA:
                newFileDir = Constants.INCOMING_FOLDER + srcId;
                sharedPrefKeyForVisualMedia = SharedPrefUtils.CALLER_MEDIA_FILEPATH;
                sharedPrefKeyForAudioMedia = SharedPrefUtils.RINGTONE_FILEPATH;
                break;
            case PROFILE_MEDIA:
                newFileDir = Constants.OUTGOING_FOLDER + srcId;
                sharedPrefKeyForVisualMedia = SharedPrefUtils.PROFILE_MEDIA_FILEPATH;
                sharedPrefKeyForAudioMedia = SharedPrefUtils.FUNTONE_FILEPATH;
                break;

            default:
                throw new UnsupportedOperationException("Invalid SpecialMediaType received");
        }

        newFileFullPath = newFileDir + "/" + srcWithExtension;
    }

    private void copyToHistoryForGalleryShow(Context context, PendingDownloadData downloadData) {

        try {

            File downloadedFile = new File(newFileFullPath);
            String extension = downloadData.getMediaFile().getExtension();
            String md5 = downloadData.getMediaFile().getMd5();
            MediaFile.FileType fileType = downloadData.getMediaFile().getFileType();

            if (MediaFilesUtils.doesFileExistInHistoryFolderByMD5(md5, Constants.HISTORY_FOLDER) || MediaFilesUtils.doesFileExistInHistoryFolderByMD5(md5, Constants.AUDIO_HISTORY_FOLDER))
                return;

            String contactName = ContactsUtils.getContactName(context, downloadData.getSourceId());

            if (contactName.isEmpty())
                contactName = downloadData.getSourceId();

            String currentDateTimeString = new SimpleDateFormat("dd_MM_yy_HHmmss").format(new Date());

            String historyFileName = "";
            if (fileType == MediaFile.FileType.AUDIO) {
                historyFileName = Constants.AUDIO_HISTORY_FOLDER + currentDateTimeString + "_" + contactName + "_" + md5 + "." + extension; //give a unique name to the file and make sure there won't be any duplicates
                SharedPrefUtils.setBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_HISTORY_EXIST, true);
            } else
                historyFileName = Constants.HISTORY_FOLDER + currentDateTimeString + "_" + contactName + "_" + md5 + "." + extension; //give a unique name to the file and make sure there won't be any duplicates

            File copyToHistoryFile = new File(historyFileName);

            if (!copyToHistoryFile.exists()) // if the file exist don't do any duplicate
            {
                FileUtils.copyFile(downloadedFile, copyToHistoryFile);
                Crashlytics.log(Log.INFO, TAG, "Creating a unique md5 file in the History Folder fileName:  " + copyToHistoryFile.getName());
                if (fileType == MediaFile.FileType.AUDIO) {
                    return;
                }

                MediaFilesUtils.triggerMediaScanOnFile(context, copyToHistoryFile);
            } else {
                Crashlytics.log(Log.ERROR, TAG, "File already exist: " + historyFileName);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
