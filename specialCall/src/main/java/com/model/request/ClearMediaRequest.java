package com.model.request;


import com.data.objects.SpecialMediaType;

/**
 * Created by Mor on 17/12/2016.
 */
public class ClearMediaRequest extends Request {

    private String destinationId;

    private SpecialMediaType specialMediaType;

    private String sourceId;

    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    public SpecialMediaType getSpecialMediaType() {
        return specialMediaType;
    }

    public void setSpecialMediaType(SpecialMediaType specialMediaType) {
        this.specialMediaType = specialMediaType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }
}
