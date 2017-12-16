package com_international.dao;

import com_international.enums.SpecialMediaType;
import com_international.files.media.MediaFile;
import com_international.utils.MediaFileUtils;
import com_international.utils.UtilityFactory;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com_international.data.objects.Constants.DEFAULT_INCOMING_FOLDER;
import static com_international.data.objects.Constants.DEFAULT_OUTGOING_FOLDER;
import static com_international.data.objects.Constants.INCOMING_FOLDER;
import static com_international.data.objects.Constants.OUTGOING_FOLDER;

/**
 * Created by Mor on 31/05/2017.
 */

public class MediaDAOImpl implements MediaDAO {

    private static final Map<SpecialMediaType, String> specialMediaType2FolderMap = new HashMap<SpecialMediaType, String>() {{
        put(SpecialMediaType.CALLER_MEDIA, INCOMING_FOLDER);
        put(SpecialMediaType.PROFILE_MEDIA, OUTGOING_FOLDER);
        put(SpecialMediaType.DEFAULT_CALLER_MEDIA, DEFAULT_INCOMING_FOLDER);
        put(SpecialMediaType.DEFAULT_PROFILE_MEDIA, DEFAULT_OUTGOING_FOLDER);
    }};

    private MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);


    @Override
    public Void addMedia(SpecialMediaType specialMediaType, String phoneNumber, MediaFile mediaFile) {
        return null;
    }

    @Override
    public void removeMedia(SpecialMediaType specialMediaType, String phoneNumber) {
        String mediaDir = specialMediaType2FolderMap.get(specialMediaType);
        String folderPath = mediaDir + phoneNumber;
        try {
            FileUtils.deleteDirectory(new File(folderPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeMedia(MediaFile mediaFile) {
        mediaFileUtils.delete(mediaFile.getFile());
    }

    @Override
    public void removeMedia(SpecialMediaType specialMediaType, MediaFile.FileType fileType, String phoneNumber) {
        String mediaDir = specialMediaType2FolderMap.get(specialMediaType);
        removeFileByType(mediaDir, phoneNumber, fileType);
    }

    @Override
    public List<MediaFile> getMedia(SpecialMediaType specialMediaType, String phoneNumber) {
        String mediaDir = specialMediaType2FolderMap.get(specialMediaType);
        return getMediaFiles(mediaDir, phoneNumber);
    }

    private List<MediaFile> getMediaFiles(String rootDir, String folderName) {
        List<MediaFile> incomingMediaFiles = null;

        if (folderName != null) {
            String folderPath = rootDir + folderName;
            File folder = new File(folderPath);
            if (folder.exists()) {
                incomingMediaFiles = new ArrayList<>();
                File[] files = folder.listFiles();
                for (File file : files) {
                    MediaFile mediaFile = new MediaFile(file, true);
                    incomingMediaFiles.add(mediaFile);
                }
            }
        }
        return incomingMediaFiles;
    }

    private void removeFileByType(String rootDir, String folderName, MediaFile.FileType fileType) {
        if (folderName != null) {
            String folderPath = rootDir + folderName;
            File folder = new File(folderPath);
            if (folder.exists()) {
                File[] files = folder.listFiles();
                for (File file : files) {
                    MediaFile mediaFile = new MediaFile(file);
                    if (mediaFile.getFileType().equals(fileType)) {
                        mediaFileUtils.delete(mediaFile);
                    }
                }
            }
        }
    }
}
