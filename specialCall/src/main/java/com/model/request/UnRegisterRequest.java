package com.model.request;

/**
 * Created by Mor on 17/12/2016.
 */

public class UnRegisterRequest extends Request {

    private String pushToken;

    @Override
    public String getPushToken() {
        return pushToken;
    }

    @Override
    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }
}


