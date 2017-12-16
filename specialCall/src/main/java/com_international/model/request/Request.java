package com_international.model.request;

import com_international.data.objects.User;

/**
 * Created by Mor on 17/12/2016.
 */
public class Request {

    private User user;
    private String locale;

    public void copy(Request request) {
        request.setUser(user);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
