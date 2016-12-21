package com.model.request;


import com.data.objects.SpecialMediaType;
import com.files.media.MediaFile;

/**
 * Created by Mor on 17/12/2016.
 */
public class UploadFileRequest extends Request {

    private String sourceId;
    private String sourceLocale;
    private String destinationId;
    private String destinationContactName;
    private MediaFile mediaFile;
    private String filePathOnSrcSd;
    private SpecialMediaType specialMediaType;
    private String sourceWithExtension;

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

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public String getFilePathOnSrcSd() {
        return filePathOnSrcSd;
    }

    public void setFilePathOnSrcSd(String filePathOnSrcSd) {
        this.filePathOnSrcSd = filePathOnSrcSd;
    }

    public SpecialMediaType getSpecialMediaType() {
        return specialMediaType;
    }

    public void setSpecialMediaType(SpecialMediaType specialMediaType) {
        this.specialMediaType = specialMediaType;
    }

    public String getSourceWithExtension() {
        return sourceWithExtension;
    }

    public void setSourceWithExtension(String sourceWithExtension) {
        this.sourceWithExtension = sourceWithExtension;
    }
}
