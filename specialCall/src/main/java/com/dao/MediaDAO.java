package com.dao;

import com.enums.SpecialMediaType;
import com.files.media.MediaFile;

import java.util.List;

/**
 * Created by Mor on 31/05/2017.
 */

public interface MediaDAO extends DAO {

    Void addMedia(SpecialMediaType specialMediaType, String phoneNumber, MediaFile mediaFile);

    void removeMedia(SpecialMediaType specialMediaType, String phoneNumber);

    void removeMedia(MediaFile mediaFile);

    void removeMedia(SpecialMediaType specialMediaType, MediaFile.FileType fileType, String phoneNumber);

    List<MediaFile> getMedia(SpecialMediaType specialMediaType, String phoneNumber);
}
