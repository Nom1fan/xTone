package com.model.request;

/**
 * Created by Mor on 17/12/2016.
 */
public class RegisterRequest extends Request {

    public RegisterRequest(Request request) {
        request.copy(this);
    }

    private int smsCode;

    private String deviceModel;

    public int getSmsCode() {
        return smsCode;
    }

    public void setSmsCode(int smsCode) {
        this.smsCode = smsCode;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }
}


