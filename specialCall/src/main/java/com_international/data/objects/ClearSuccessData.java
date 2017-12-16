package com_international.data.objects;

import com_international.enums.SpecialMediaType;
import com_international.model.push.AbstractPushData;

/**
 * Created by Mor on 1/3/2017.
 */

public class ClearSuccessData extends AbstractPushData {

    private String destinationId;
    private SpecialMediaType specialMediaType;

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
}
