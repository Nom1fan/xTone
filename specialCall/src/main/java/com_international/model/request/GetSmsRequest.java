package com_international.model.request;

/**
 * Created by Mor on 17/12/2016.
 */
public class GetSmsRequest extends Request {

    private String internationalPhoneNumber;

    public GetSmsRequest(Request request) {
        request.copy(this);
    }

    public String getInternationalPhoneNumber() {
        return internationalPhoneNumber;
    }

    public void setInternationalPhoneNumber(String internationalPhoneNumber) {
        this.internationalPhoneNumber = internationalPhoneNumber;
    }
}


