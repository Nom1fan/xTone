package com.data.objects;

import com.enums.SpecialMediaType;
import com.files.media.MediaFile;
import com.model.push.AbstractPushData;

/**
 * Created by Mor on 12/31/2016.
 */
public class PendingDownloadData extends AbstractPushData {

    private String sourceId;
    private String sourceLocale;
    private String destinationId;
    private String destinationContactName;
    private SpecialMediaType specialMediaType;
    private String filePathOnServer;
    private String filePathOnSrcSd;
    private MediaFile mediaFile;
    private int commId;

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceLocale() {
        return sourceLocale;
    }

    public void setSourceLocale(String sourceLocale) {
        this.sourceLocale = sourceLocale;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    public String getDestinationContactName() {
        return destinationContactName;
    }

    public void setDestinationContactName(String destinationContactName) {
        this.destinationContactName = destinationContactName;
    }

    public SpecialMediaType getSpecialMediaType() {
        return specialMediaType;
    }

    public void setSpecialMediaType(SpecialMediaType specialMediaType) {
        this.specialMediaType = specialMediaType;
    }

    public String getFilePathOnServer() {
        return filePathOnServer;
    }

    public void setFilePathOnServer(String filePathOnServer) {
        this.filePathOnServer = filePathOnServer;
    }

    public String getFilePathOnSrcSd() {
        return filePathOnSrcSd;
    }

    public void setFilePathOnSrcSd(String filePathOnSrcSd) {
        this.filePathOnSrcSd = filePathOnSrcSd;
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public int getCommId() {
        return commId;
    }

    public void setCommId(int commId) {
        this.commId = commId;
    }

    @Override
    public String toString() {
        return "PendingDownloadData{" +
                "sourceId='" + sourceId + '\'' +
                ", sourceLocale='" + sourceLocale + '\'' +
                ", destinationId='" + destinationId + '\'' +
                ", destinationContactName='" + destinationContactName + '\'' +
                ", specialMediaType=" + specialMediaType +
                ", filePathOnServer='" + filePathOnServer + '\'' +
                ", filePathOnSrcSd='" + filePathOnSrcSd + '\'' +
                ", mediaFile=" + mediaFile +
                ", commId=" + commId +
                '}';
    }
}
