package com.model.request;

import com.enums.SpecialMediaType;

/**
 * Created by Mor on 17/12/2016.
 */
public class GetDefaultMediaDataRequest extends Request {

    private String phoneNumber;

    private SpecialMediaType specialMediaType;

    public GetDefaultMediaDataRequest(Request request) {
        request.copy(this);
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public SpecialMediaType getSpecialMediaType() {
        return specialMediaType;
    }

    public void setSpecialMediaType(SpecialMediaType specialMediaType) {
        this.specialMediaType = specialMediaType;
    }
}


