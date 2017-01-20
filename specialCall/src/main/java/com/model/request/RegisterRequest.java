package com.model.request;

/**
 * Created by Mor on 17/12/2016.
 */
public class RegisterRequest extends Request {

    private int smsCode;

    public RegisterRequest(Request request) {
        request.copy(this);
    }

    public int getSmsCode() {
        return smsCode;
    }

    public void setSmsCode(int smsCode) {
        this.smsCode = smsCode;
    }
}


