package com.data.objects;

import com.enums.SpecialMediaType;
import com.files.media.MediaFile;

import java.io.Serializable;

/**
 * Created by Mor on 24/05/2017.
 */

public class DefaultMediaData implements Serializable {

    private MediaFile mediaFile;

    private long defaultMediaUnixTime;

    private String filePathOnServer;

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
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

    @Override
    public String toString() {
        return "DefaultMediaData{" +
                "mediaFile=" + mediaFile +
                ", defaultMediaUnixTime=" + defaultMediaUnixTime +
                ", filePathOnServer='" + filePathOnServer + '\'' +
                '}';
    }
}
