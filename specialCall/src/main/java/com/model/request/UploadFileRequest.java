package com.model.request;


import com.data.objects.SpecialMediaType;
import com.files.media.MediaFile;

/**
 * Created by Mor on 17/12/2016.
 */
public class UploadFileRequest extends Request {

    private String sourceId;
    private String destinationId;
    private String destinationContactName;
    private MediaFile mediaFileDTO;
    private String filePathOnSrcSd;
    private SpecialMediaType specialMediaType;

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
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

    public MediaFile getMediaFileDTO() {
        return mediaFileDTO;
    }

    public void setMediaFileDTO(MediaFile mediaFileDTO) {
        this.mediaFileDTO = mediaFileDTO;
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
}
