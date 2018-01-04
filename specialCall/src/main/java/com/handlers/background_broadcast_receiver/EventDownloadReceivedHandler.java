package com.handlers.background_broadcast_receiver;

import android.content.Context;

import com.data.objects.Constants;
import com.data.objects.Contact;
import com.data.objects.DownloadData;
import com.data.objects.PendingDownloadData;
import com.enums.SaveMediaOption;
import com.enums.SpecialMediaType;
import com.event.EventReport;
import com.exceptions.FailedToSetNewMediaException;
import com.files.media.MediaFile;
import com.handlers.Handler;
import com.logger.Logger;
import com.logger.LoggerFactory;
import com.services.ServerProxyService;
import com.utils.ContactsUtils;
import com.utils.MediaFileUtils;
import com.utils.Phone2MediaPathMapperUtils;
import com.utils.PhoneNumberUtils;
import com.utils.SettingsUtils;
import com.utils.SharedPrefUtils;
import com.utils.UtilityFactory;

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
    private MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);

    private final ContactsUtils contactsUtils = UtilityFactory.instance().getUtility(ContactsUtils.class);


    private Logger logger = LoggerFactory.getLogger();

    @Override
    public void handle(Context ctx, Object... params) {
        EventReport eventReport = (EventReport) params[0];

        logger.info(TAG, "In: Download complete");

        DownloadData downloadData = (DownloadData) eventReport.data();
        preparePathsAndDirs(downloadData);
        PendingDownloadData pendingDownloadData = downloadData.getPendingDownloadData();


        // Copy new downloaded file to History Folder so it will show up in Gallery and don't make any duplicates with MD5 signature
        if (isAuthorizedToLeaveMedia(ctx, pendingDownloadData.getSourceId())) {
            copyToHistoryForGalleryShow(ctx, pendingDownloadData);
        }

        MediaFile.FileType fType = pendingDownloadData.getMediaFile().getFileType();
        String fFullName = pendingDownloadData.getMediaFile().getFile().getName();
        String source = pendingDownloadData.getSourceId();
        String md5 = pendingDownloadData.getMediaFile().getMd5();
        String source6Digit = source;
        if (source.length()>6) {
            source6Digit = source.substring(source.length() - 6); /// TODO:  ADDED THIS FOR INTERNATIONAL OPTIONS (HACKED)
        }

        try {

            switch (fType) {
                case AUDIO:
                    setNewAudioMedia(ctx, source6Digit, md5);
                    mediaFileUtils.deleteFilesIfNecessary(ctx, sharedPrefKeyForAudioMedia, newFileDir, fFullName, fType, source6Digit);
                    break;

                case VIDEO:
                case IMAGE:
                    setNewVisualMedia(ctx, source6Digit, md5);
                    mediaFileUtils.deleteFilesIfNecessary(ctx, sharedPrefKeyForVisualMedia, newFileDir, fFullName, fType, source6Digit);
                    break;
            }

            if (shouldNotifyMediaReady(downloadData.getPendingDownloadData().getSpecialMediaType())) {
                ServerProxyService.notifyMediaReady(ctx, pendingDownloadData);
            }

        } catch (FailedToSetNewMediaException e) {
            //TODO Inform source of failure
        }
    }

    private boolean shouldNotifyMediaReady(SpecialMediaType specialMediaType) {
        return specialMediaType.equals(SpecialMediaType.CALLER_MEDIA) || specialMediaType.equals(SpecialMediaType.PROFILE_MEDIA);
    }

    private boolean isAuthorizedToLeaveMedia(Context context, String incomingNumber) {
        SaveMediaOption saveMediaOption = SettingsUtils.getSaveMediaOption(context);
        if (saveMediaOption.equals(SaveMediaOption.ALWAYS)) {
            return true;
        } else if (saveMediaOption.equals(SaveMediaOption.CONTACTS_ONLY)) {
            List<Contact> contactsList = contactsUtils.getAllContacts(context);
            List<String> contactPhoneNumbers = new ArrayList<>();

            for (int i = 0; i < contactsList.size(); i++) {
                contactPhoneNumbers.add(contactsList.get(i).getPhoneNumber());
            }

            return contactPhoneNumbers.contains(PhoneNumberUtils.toValidLocalPhoneNumber(incomingNumber));
        } else // Never save
            return false;
    }

    private void setNewAudioMedia(Context context, String source, String md5) throws FailedToSetNewMediaException {

        logger.info(TAG, "setNewAudioMedia with key:[" + sharedPrefKeyForAudioMedia + "], number:[" + source + "] and path:[" + newFileFullPath + "]");

        if (!mediaFileUtils.isAudioFileCorrupted(newFileFullPath, context)) {
            SharedPrefUtils.setString(context,
                    sharedPrefKeyForAudioMedia, source, newFileFullPath);

            // Backing up audio file md5
            SharedPrefUtils.setString(context,
                    sharedPrefKeyForAudioMedia, newFileFullPath, md5);
        } else {
            logger.error(TAG, "CORRUPTED FILE : setNewAudioMedia with sharedPrefs: " + newFileFullPath);
            throw new FailedToSetNewMediaException();
        }
    }

    private void setNewVisualMedia(Context context, String source, String md5) throws FailedToSetNewMediaException {

        logger.info(TAG, "setNewVisualMedia with key:[" + sharedPrefKeyForVisualMedia + "], number:[" + source + "] and path:[" + newFileFullPath + "]");

        if (!mediaFileUtils.isVideoFileCorrupted(newFileFullPath, context)) {
            SharedPrefUtils.setString(context, sharedPrefKeyForVisualMedia, source, newFileFullPath);

            // Backing up visual file md5
            SharedPrefUtils.setString(context, sharedPrefKeyForVisualMedia, newFileFullPath, md5);
        } else {
            logger.error(TAG, "CORRUPTED FILE : setNewVisualMedia with sharedPrefs: " + newFileFullPath);
            throw new FailedToSetNewMediaException();
        }
    }

    private void preparePathsAndDirs(DownloadData downloadData) {
        PendingDownloadData pendingDownloadData = downloadData.getPendingDownloadData();

        SpecialMediaType specialMediaType = pendingDownloadData.getSpecialMediaType();
        String srcId = pendingDownloadData.getSourceId();

        // Preparing for appropriate special media type
        switch (specialMediaType) {
            case CALLER_MEDIA:
                newFileDir = Constants.INCOMING_FOLDER + srcId;
                sharedPrefKeyForVisualMedia = Phone2MediaPathMapperUtils.CALLER_VISUAL_MEDIA;
                sharedPrefKeyForAudioMedia = Phone2MediaPathMapperUtils.CALLER_AUDIO_MEDIA;
                newFileFullPath = mediaFileUtils.resolvePathBySpecialMediaType(pendingDownloadData);
                break;
            case PROFILE_MEDIA:
                newFileDir = Constants.OUTGOING_FOLDER + srcId;
                sharedPrefKeyForVisualMedia = Phone2MediaPathMapperUtils.PROFILE_VISUAL_MEDIA;
                sharedPrefKeyForAudioMedia = Phone2MediaPathMapperUtils.PROFILE_AUDIO_MEDIA;
                newFileFullPath = mediaFileUtils.resolvePathBySpecialMediaType(pendingDownloadData);
                break;
            case DEFAULT_CALLER_MEDIA:
                newFileDir = Constants.DEFAULT_INCOMING_FOLDER + srcId;
                sharedPrefKeyForVisualMedia = Phone2MediaPathMapperUtils.DEFAULT_CALLER_VISUAL_MEDIA;
                sharedPrefKeyForAudioMedia = Phone2MediaPathMapperUtils.DEFAULT_CALLER_AUDIO_MEDIA;
                newFileFullPath = mediaFileUtils.resolvePathBySpecialMediaType(pendingDownloadData.getSourceId(), specialMediaType, downloadData.getDefaultMediaData());
                break;
            case DEFAULT_PROFILE_MEDIA:
                newFileDir = Constants.DEFAULT_OUTGOING_FOLDER + srcId;
                sharedPrefKeyForVisualMedia = Phone2MediaPathMapperUtils.DEFAULT_PROFILE_VISUAL_MEDIA;
                sharedPrefKeyForAudioMedia = Phone2MediaPathMapperUtils.DEFAULT_PROFILE_AUDIO_MEDIA;
                newFileFullPath = mediaFileUtils.resolvePathBySpecialMediaType(pendingDownloadData.getSourceId(), specialMediaType, downloadData.getDefaultMediaData());
                break;

            default:
                throw new UnsupportedOperationException("Invalid SpecialMediaType received");
        }

    }

    private void copyToHistoryForGalleryShow(Context context, PendingDownloadData downloadData) {

        try {

            File downloadedFile = new File(newFileFullPath);
            String extension = downloadData.getMediaFile().getExtension();
            String md5 = downloadData.getMediaFile().getMd5();
            MediaFile.FileType fileType = downloadData.getMediaFile().getFileType();

            if (md5 == null || md5.isEmpty()) {
                md5 = mediaFileUtils.getMD5(downloadData.getMediaFile().getFile().getAbsolutePath());
            }

            if (mediaFileUtils.doesFileExistInHistoryFolderByMD5(md5, Constants.HISTORY_FOLDER) || mediaFileUtils.doesFileExistInHistoryFolderByMD5(md5, Constants.AUDIO_HISTORY_FOLDER)) {
                return;
            }

            String contactName = contactsUtils.getContactName(context, downloadData.getSourceId());

            if (contactName != null && contactName.isEmpty()) {
                contactName = downloadData.getSourceId();
            }

            String currentDateTimeString = new SimpleDateFormat("dd_MM_yy_HHmmss").format(new Date());

            String historyFileName;
            if (fileType == MediaFile.FileType.AUDIO) {
                historyFileName = Constants.AUDIO_HISTORY_FOLDER + currentDateTimeString + "_" + contactName + "_" + md5 + "." + extension; //give a unique name to the file and make sure there won't be any duplicates
                SharedPrefUtils.setBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_HISTORY_EXIST, true);
            } else
                historyFileName = Constants.HISTORY_FOLDER + currentDateTimeString + "_" + contactName + "_" + md5 + "." + extension; //give a unique name to the file and make sure there won't be any duplicates

            File copyToHistoryFile = new File(historyFileName);

            if (!copyToHistoryFile.exists()) // if the file exist don't do any duplicate
            {
                FileUtils.copyFile(downloadedFile, copyToHistoryFile);
                logger.info(TAG, "Creating a unique md5 file in the History Folder fileName:  " + copyToHistoryFile.getName());
                if (fileType == MediaFile.FileType.AUDIO) {
                    return;
                }

                mediaFileUtils.triggerMediaScanOnFile(context, copyToHistoryFile);
            } else {
                logger.error(TAG, "File already exist: " + historyFileName);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
