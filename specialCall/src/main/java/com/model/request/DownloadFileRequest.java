package com.model.request;

import com.data.objects.SpecialMediaType;
import com.files.media.MediaFile;

/**
 * Created by Mor on 17/12/2016.
 */
public class DownloadFileRequest extends Request {

    private int commId;
    private String sourceId;
    private String destinationId;
    private String destinationContactName;
    private String filePathOnServer;
    private SpecialMediaType specialMediaType;
    private String filePathOnSrcSd;

    public DownloadFileRequest(Request request) {
        request.copy(this);
    }

    public int getCommId() {
        return commId;
    }

    public void setCommId(int commId) {
        this.commId = commId;
    }

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

    public String getFilePathOnSrcSd() {
        return filePathOnSrcSd;
    }

    public void setFilePathOnSrcSd(String filePathOnSrcSd) {
        this.filePathOnSrcSd = filePathOnSrcSd;
    }
}
