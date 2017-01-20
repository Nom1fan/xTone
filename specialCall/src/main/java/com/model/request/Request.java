package com.model.request;

/**
 * Created by Mor on 17/12/2016.
 */
public class Request {

    private String messageInitiaterId;
    private String pushToken;
    private String androidVersion;
    private String iosVersion;
    private String appVersion;
    private String sourceLocale;
    private String deviceModel;

    public void copy(Request request) {
        request.setMessageInitiaterId(getMessageInitiaterId());
        request.setSourceLocale(getSourceLocale());
        request.setAppVersion(getAppVersion());
        request.setIosVersion(getIosVersion());
        request.setAndroidVersion(getAndroidVersion());
        request.setPushToken(getPushToken());
        request.setDeviceModel(getDeviceModel());
    }


    public String getMessageInitiaterId() {
        return messageInitiaterId;
    }

    public void setMessageInitiaterId(String messageInitiaterId) {
        this.messageInitiaterId = messageInitiaterId;
    }

    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public String getAndroidVersion() {
        return androidVersion;
    }

    public void setAndroidVersion(String androidVersion) {
        this.androidVersion = androidVersion;
    }

    public String getIosVersion() {
        return iosVersion;
    }

    public void setIosVersion(String iosVersion) {
        this.iosVersion = iosVersion;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getSourceLocale() {
        return sourceLocale;
    }

    public void setSourceLocale(String sourceLocale) {
        this.sourceLocale = sourceLocale;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }
}
