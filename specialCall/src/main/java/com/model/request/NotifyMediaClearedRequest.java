package com.model.request;


import com.data.objects.SpecialMediaType;

/**
 * Created by Mor on 17/12/2016.
 */
public class NotifyMediaClearedRequest extends Request {

    private String sourceId;
    private String destinationContactName;
    private SpecialMediaType specialMediaType;
    private String sourceLocale;

    public NotifyMediaClearedRequest(Request request) {
        request.copy(this);
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
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

    public String getSourceLocale() {
        return sourceLocale;
    }

    public void setSourceLocale(String sourceLocale) {
        this.sourceLocale = sourceLocale;
    }
}
