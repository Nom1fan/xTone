package com.model.request;

/**
 * Created by Mor on 17/12/2016.
 */
public class GetSmsRequest extends Request {

    public GetSmsRequest(Request request) {
        request.copy(this);
    }

    private String internationalPhoneNumber;

    public String getInternationalPhoneNumber() {
        return internationalPhoneNumber;
    }

    public void setInternationalPhoneNumber(String internationalPhoneNumber) {
        this.internationalPhoneNumber = internationalPhoneNumber;
    }
}


