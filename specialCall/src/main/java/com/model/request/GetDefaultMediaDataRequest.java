package com.model.request;

import com.enums.SpecialMediaType;

import java.util.List;

/**
 * Created by Mor on 17/12/2016.
 */
public class GetDefaultMediaDataRequest extends Request {

    private List<String> contactUids;

    private SpecialMediaType specialMediaType;

    public GetDefaultMediaDataRequest(Request request) {
        request.copy(this);
    }

    public List<String> getContactUids() {
        return contactUids;
    }

    public void setContactUids(List<String> contactUids) {
        this.contactUids = contactUids;
    }

    public SpecialMediaType getSpecialMediaType() {
        return specialMediaType;
    }

    public void setSpecialMediaType(SpecialMediaType specialMediaType) {
        this.specialMediaType = specialMediaType;
    }
}


