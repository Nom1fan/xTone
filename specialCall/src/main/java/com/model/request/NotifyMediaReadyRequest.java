package com.model.request;


import com.data.objects.SpecialMediaType;

/**
 * Created by Mor on 17/12/2016.
 */

public class NotifyMediaReadyRequest extends Request {

    private int commId;

    private String sourceId;

    private String destinationId;

    private String destinationContactName;

    private SpecialMediaType specialMediaType;

    private String filePathOnSrcSd;

    public NotifyMediaReadyRequest(Request request) {
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
