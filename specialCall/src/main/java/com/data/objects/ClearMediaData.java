package com.data.objects;

import com.files.media.MediaFile;
import com.model.push.AbstractPushData;

/**
 * Created by Mor on 12/31/2016.
 */
public class ClearMediaData extends AbstractPushData {

    private String sourceId;
    private String sourceLocale;
    private SpecialMediaType specialMediaType;

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

    public SpecialMediaType getSpecialMediaType() {
        return specialMediaType;
    }

    public void setSpecialMediaType(SpecialMediaType specialMediaType) {
        this.specialMediaType = specialMediaType;
    }
}
