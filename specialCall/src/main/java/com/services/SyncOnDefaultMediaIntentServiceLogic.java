package com.services;

import android.content.Context;

import com.client.Client;
import com.client.ClientFactory;
import com.client.DefaultMediaClient;
import com.client.UsersClient;
import com.converters.MediaDataConverter;
import com.dao.DAOFactory;
import com.dao.MediaDAO;
import com.data.objects.Contact;
import com.data.objects.DefaultMediaData;
import com.data.objects.DefaultMediaDataContainer;
import com.data.objects.PendingDownloadData;
import com.data.objects.User;
import com.enums.SpecialMediaType;
import com.files.media.MediaFile;
import com.logger.Logger;
import com.logger.LoggerFactory;
import com.utils.ContactsUtils;
import com.utils.MediaFileUtils;
import com.utils.UtilityFactory;

import java.io.IOException;
import java.util.List;

import static com.enums.SpecialMediaType.DEFAULT_CALLER_MEDIA;
import static com.enums.SpecialMediaType.DEFAULT_PROFILE_MEDIA;

/**
 * Created by Mor on 25/05/2017.
 */

public class SyncOnDefaultMediaIntentServiceLogic {

    private static final String TAG = SyncOnDefaultMediaIntentServiceLogic.class.getSimpleName();

    private static final Logger logger = LoggerFactory.getLogger();

    private Context context;

    private DefaultMediaClient defaultMediaClient;

    private MediaDAO mediaDAO;

    private ServerProxy serverProxy;

    private MediaDataConverter mediaDataConverter;

    private MediaFileUtils mediaFileUtils;

    private ContactsUtils contactsUtils;

    public SyncOnDefaultMediaIntentServiceLogic() {

    }

    public SyncOnDefaultMediaIntentServiceLogic(Context context, ServerProxy serverProxy) {
        this.context = context;
        this.serverProxy = serverProxy;
        this.mediaDAO = DAOFactory.getDAO(MediaDAO.class);
        this.defaultMediaClient = ClientFactory.getInstance().getClient(DefaultMediaClient.class);
        this.mediaDataConverter = UtilityFactory.instance().getUtility(MediaDataConverter.class);
        this.mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);
        this.contactsUtils = UtilityFactory.instance().getUtility(ContactsUtils.class);
    }

    public ContactsUtils getContactsUtils() {
        return contactsUtils;
    }

    public void setContactsUtils(ContactsUtils contactsUtils) {
        this.contactsUtils = contactsUtils;
    }

    public void performSyncOnDefaultMedia() {

        try {
            syncOnDefaultMedia(DEFAULT_CALLER_MEDIA);
            syncOnDefaultMedia(DEFAULT_PROFILE_MEDIA);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void syncOnDefaultMedia(SpecialMediaType specialMediaType) throws IOException {
        List<String> contactsUids = contactsUtils.convertToUids(contactsUtils.getAllContacts(context));
        List<DefaultMediaDataContainer> defaultMediaDataContainers = defaultMediaClient.getDefaultMediaData(context, contactsUids, specialMediaType);
        if (defaultMediaDataContainers != null) {
            for (DefaultMediaDataContainer defaultMediaDataContainer : defaultMediaDataContainers) {
                logger.debug(TAG, "Sync default media for contact:" + defaultMediaDataContainer);

                String phoneNumber = defaultMediaDataContainer.getUid();
                List<DefaultMediaData> defaultMediaDataList = defaultMediaDataContainer.getDefaultMediaDataList();
                List<MediaFile> mediaFiles = mediaDAO.getMedia(specialMediaType, phoneNumber);

                if (defaultMediaDataList == null || defaultMediaDataList.isEmpty()) {
                    logger.debug(TAG, "For contact:[" + phoneNumber + "] Clearing all default media of type:[" + specialMediaType + "]");
                    mediaDAO.removeMedia(specialMediaType, phoneNumber);
                } else if (mediaFiles == null || mediaFiles.isEmpty()) {
                    // Initial download for contact defaults
                    for (DefaultMediaData defaultMediaData : defaultMediaDataList) {
                        downloadDefaultMedia(phoneNumber, specialMediaType, defaultMediaData);
                    }
                } else {
                    syncFiles(phoneNumber, specialMediaType, defaultMediaDataList, mediaFiles);
                    syncNewDefaultMedia(phoneNumber, specialMediaType, defaultMediaDataList, mediaFiles);
                }
            }
        }
    }

    private void syncNewDefaultMedia(String phoneNumber, SpecialMediaType specialMediaType, List<DefaultMediaData> defaultMediaDataList, List<MediaFile> mediaFiles) {
        for (DefaultMediaData defaultMediaData : defaultMediaDataList) {
            boolean isFound = doesDefaultMediaExistsInFiles(mediaFiles, defaultMediaData);
            if (!isFound) {
                downloadDefaultMedia(phoneNumber, specialMediaType, defaultMediaData);
            }
        }
    }

    private void syncFiles(String phoneNumber, SpecialMediaType specialMediaType, List<DefaultMediaData> defaultMediaDataList, List<MediaFile> mediaFiles) {
        for (MediaFile mediaFile : mediaFiles) {
            syncFile(phoneNumber, specialMediaType, defaultMediaDataList, mediaFile);
        }
    }

    private void syncFile(String phoneNumber, SpecialMediaType specialMediaType, List<DefaultMediaData> defaultMediaDataList, MediaFile mediaFile) {
        boolean wasRemoved = syncRemove(defaultMediaDataList, mediaFile);
        if (wasRemoved) {
            return;
        }

        MediaFile.FileType fileType = mediaFile.getFileType();
        for (DefaultMediaData defaultMediaData : defaultMediaDataList) {
            if (defaultMediaData.getMediaFile().getFileType().equals(fileType)) {
                long savedFileUnixTime = mediaFileUtils.getFileCreationDateInUnixTime(mediaFile);
                long latestFileMediaUnixTime = defaultMediaData.getDefaultMediaUnixTime();

                // Need to sync
                if (latestFileMediaUnixTime > savedFileUnixTime) {
                    downloadDefaultMedia(phoneNumber, specialMediaType, defaultMediaData);
                }
            }
        }
    }

    private boolean syncRemove(List<DefaultMediaData> defaultMediaDataList, MediaFile mediaFile) {
        boolean wasRemoved = false;
        boolean isFound = doesFileTypeExistsInDefaultMedia(defaultMediaDataList, mediaFile);
        if (!isFound) {
            mediaDAO.removeMedia(mediaFile);
            wasRemoved = true;
        }
        return wasRemoved;
    }

    private boolean doesDefaultMediaExistsInFiles(List<MediaFile> mediaFiles, DefaultMediaData defaultMediaData) {
        boolean isFound = false;
        for (MediaFile mediaFile : mediaFiles) {
            if (mediaFile.getFileType().equals(defaultMediaData.getMediaFile().getFileType())) {
                isFound = true;
            }
        }
        return isFound;
    }

    private boolean doesFileTypeExistsInDefaultMedia(List<DefaultMediaData> defaultMediaDataList, MediaFile mediaFile) {
        boolean isFound = false;
        for (DefaultMediaData defaultMediaData : defaultMediaDataList) {
            MediaFile.FileType defaultFileType = defaultMediaData.getMediaFile().getFileType();
            if (defaultFileType.equals(mediaFile.getFileType())) {
                isFound = true;
            }
        }
        return isFound;
    }

    private void downloadDefaultMedia(String phoneNumber, SpecialMediaType specialMediaType, DefaultMediaData defaultMediaData) {
        logger.info(TAG, "Downloading default media for contact:[" + phoneNumber + "]. " + defaultMediaData);
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(phoneNumber, specialMediaType, defaultMediaData);
        serverProxy.sendActionDownload(context, pendingDownloadData, defaultMediaData);
    }

    //region Getters & Setters
    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public static Logger getLogger() {
        return logger;
    }

    public DefaultMediaClient getDefaultMediaClient() {
        return defaultMediaClient;
    }

    public void setDefaultMediaClient(DefaultMediaClient defaultMediaClient) {
        this.defaultMediaClient = defaultMediaClient;
    }

    public MediaDAO getMediaDAO() {
        return mediaDAO;
    }

    public void setMediaDAO(MediaDAO mediaDAO) {
        this.mediaDAO = mediaDAO;
    }

    public ServerProxy getServerProxy() {
        return serverProxy;
    }

    public void setServerProxy(ServerProxy serverProxy) {
        this.serverProxy = serverProxy;
    }

    public MediaDataConverter getMediaDataConverter() {
        return mediaDataConverter;
    }

    public void setMediaDataConverter(MediaDataConverter mediaDataConverter) {
        this.mediaDataConverter = mediaDataConverter;
    }

    public MediaFileUtils getMediaFileUtils() {
        return mediaFileUtils;
    }

    public void setMediaFileUtils(MediaFileUtils mediaFileUtils) {
        this.mediaFileUtils = mediaFileUtils;
    }
    //endregion
}
