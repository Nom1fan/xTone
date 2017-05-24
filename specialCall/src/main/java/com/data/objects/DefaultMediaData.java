package com.data.objects;

import com.enums.SpecialMediaType;
import com.files.media.MediaFile;

/**
 * Created by Mor on 24/05/2017.
 */

public class DefaultMediaData {

    private String uid;

    private MediaFile.FileType fileType;

    private SpecialMediaType specialMediaType;

    private long defaultMediaUnixTime;

    private String filePathOnServer;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public SpecialMediaType getSpecialMediaType() {
        return specialMediaType;
    }

    public void setSpecialMediaType(SpecialMediaType specialMediaType) {
        this.specialMediaType = specialMediaType;
    }

    public long getDefaultMediaUnixTime() {
        return defaultMediaUnixTime;
    }

    public void setDefaultMediaUnixTime(long defaultMediaUnixTime) {
        this.defaultMediaUnixTime = defaultMediaUnixTime;
    }

    public MediaFile.FileType getFileType() {
        return fileType;
    }

    public void setFileType(MediaFile.FileType fileType) {
        this.fileType = fileType;
    }

    public String getFilePathOnServer() {
        return filePathOnServer;
    }

    public void setFilePathOnServer(String filePathOnServer) {
        this.filePathOnServer = filePathOnServer;
    }
}
