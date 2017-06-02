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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PendingDownloadData that = (PendingDownloadData) o;

        if (commId != that.commId) return false;
        if (sourceId != null ? !sourceId.equals(that.sourceId) : that.sourceId != null)
            return false;
        if (sourceLocale != null ? !sourceLocale.equals(that.sourceLocale) : that.sourceLocale != null)
            return false;
        if (destinationId != null ? !destinationId.equals(that.destinationId) : that.destinationId != null)
            return false;
        if (destinationContactName != null ? !destinationContactName.equals(that.destinationContactName) : that.destinationContactName != null)
            return false;
        if (specialMediaType != that.specialMediaType) return false;
        if (filePathOnServer != null ? !filePathOnServer.equals(that.filePathOnServer) : that.filePathOnServer != null)
            return false;
        if (filePathOnSrcSd != null ? !filePathOnSrcSd.equals(that.filePathOnSrcSd) : that.filePathOnSrcSd != null)
            return false;
        return mediaFile != null ? mediaFile.equals(that.mediaFile) : that.mediaFile == null;

    }

    @Override
    public int hashCode() {
        int result = sourceId != null ? sourceId.hashCode() : 0;
        result = 31 * result + (sourceLocale != null ? sourceLocale.hashCode() : 0);
        result = 31 * result + (destinationId != null ? destinationId.hashCode() : 0);
        result = 31 * result + (destinationContactName != null ? destinationContactName.hashCode() : 0);
        result = 31 * result + (specialMediaType != null ? specialMediaType.hashCode() : 0);
        result = 31 * result + (filePathOnServer != null ? filePathOnServer.hashCode() : 0);
        result = 31 * result + (filePathOnSrcSd != null ? filePathOnSrcSd.hashCode() : 0);
        result = 31 * result + (mediaFile != null ? mediaFile.hashCode() : 0);
        result = 31 * result + commId;
        return result;
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
