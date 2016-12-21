package com.model.response;

import com.data.objects.UserStatus;

/**
 * Created by Mor on 21/12/2016.
 */
public class UserDTO {

    private String uid;
    private UserStatus userStatus;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }
}
