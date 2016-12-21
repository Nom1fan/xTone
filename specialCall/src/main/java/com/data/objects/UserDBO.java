package com.data.objects;

import java.util.Date;

/**
 * Created by Mor on 01/04/2016.
 */
public class UserDBO {

    private String uid;
    private String token;
    private Date registered_date;
    private UserStatus userStatus;
    private Date unregistered_date;
    private int unregistered_count;
    private String deviceModel;
    private String androidVersion;

    public UserDBO() {
    }

    public UserDBO(
            String uid,
            String token,
            Date registered_date,
            UserStatus userStatus,
            Date unregistered_date,
            int unregistered_count,
            String deviceModel,
            String androidVersion) {

        this.uid = uid;
        this.token = token;
        this.registered_date = registered_date;
        this.userStatus = userStatus;
        this.unregistered_date = unregistered_date;
        this.unregistered_count = unregistered_count;
        this.deviceModel = deviceModel;
        this.androidVersion = androidVersion;
    }

    public String getUid() {
        return uid;
    }

    public String getToken() {
        return token;
    }

    public Date getRegistered_date() {
        return registered_date;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public Date getUnregistered_date() {
        return unregistered_date;
    }

    public int getUnregistered_count() {
        return unregistered_count;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public String getAndroidVersion() {
        return androidVersion;
    }

    public void setAndroidVersion(String androidVersion) {

        this.androidVersion = androidVersion;
    }

    public void setUid(String uid) {

        this.uid = uid;
    }
}
