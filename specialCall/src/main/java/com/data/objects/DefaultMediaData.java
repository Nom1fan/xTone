package com.data.objects;

import com.enums.SpecialMediaType;
import com.files.media.MediaFile;

import java.io.Serializable;

/**
 * Created by Mor on 24/05/2017.
 */

public class DefaultMediaData implements Serializable {

    private int commId;

    private String uid;

    MediaFile mediaFile;

    private long defaultMediaUnixTime;

    private String filePathOnServer;

    private SpecialMediaType specialMediaType;

    public int getCommId() {
        return commId;
    }

    public void setCommId(int commId) {
        this.commId = commId;
    }

    public String getUid() {
        return uid;
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public long getDefaultMediaUnixTime() {
        return defaultMediaUnixTime;
    }

    public void setDefaultMediaUnixTime(long defaultMediaUnixTime) {
        this.defaultMediaUnixTime = defaultMediaUnixTime;
    }

    public String getFilePathOnServer() {
        return filePathOnServer;
    }

    public void setFilePathOnServer(String filePathOnServer) {
        this.filePathOnServer = filePathOnServer;
    }

    public SpecialMediaType getSpecialMediaType() {
        return specialMediaType;
    }

    public void setSpecialMediaType(SpecialMediaType specialMediaType) {
        this.specialMediaType = specialMediaType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultMediaData that = (DefaultMediaData) o;

        if (commId != that.commId) return false;
        if (defaultMediaUnixTime != that.defaultMediaUnixTime) return false;
        if (uid != null ? !uid.equals(that.uid) : that.uid != null) return false;
        if (mediaFile != null ? !mediaFile.equals(that.mediaFile) : that.mediaFile != null)
            return false;
        if (filePathOnServer != null ? !filePathOnServer.equals(that.filePathOnServer) : that.filePathOnServer != null)
            return false;
        return specialMediaType == that.specialMediaType;

    }

    @Override
    public int hashCode() {
        int result = commId;
        result = 31 * result + (uid != null ? uid.hashCode() : 0);
        result = 31 * result + (mediaFile != null ? mediaFile.hashCode() : 0);
        result = 31 * result + (int) (defaultMediaUnixTime ^ (defaultMediaUnixTime >>> 32));
        result = 31 * result + (filePathOnServer != null ? filePathOnServer.hashCode() : 0);
        result = 31 * result + (specialMediaType != null ? specialMediaType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DefaultMediaData{" +
                "commId=" + commId +
                ", uid='" + uid + '\'' +
                ", mediaFile=" + mediaFile +
                ", defaultMediaUnixTime=" + defaultMediaUnixTime +
                ", filePathOnServer='" + filePathOnServer + '\'' +
                ", specialMediaType=" + specialMediaType +
                '}';
    }
}
