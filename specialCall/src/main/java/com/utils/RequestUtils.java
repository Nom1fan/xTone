package com.utils;

import android.content.Context;
import android.os.Build;

import com.data.objects.Constants;
import com.data.objects.User;
import com.model.request.Request;

/**
 * Created by Mor on 12/26/2016.
 */

public abstract class RequestUtils {
    public static void prepareDefaultRequest(Context context, Request request) {
        User user = new User();
        user.setUid(Constants.MY_ID(context));
        user.setAppVersion(String.valueOf(Constants.APP_VERSION()));
        user.setToken(Constants.MY_FIREBASE_TOKEN(context));
        user.setOs("ANDROID");
        user.setOsVersion(Build.VERSION.RELEASE);
        user.setDeviceModel(Constants.MY_DEVICE_MODEL());
        request.setUser(user);
    }

    public static Request getDefaultRequest(Context context) {
        Request request = new Request();
        prepareDefaultRequest(context, request);
        return request;
    }
}
