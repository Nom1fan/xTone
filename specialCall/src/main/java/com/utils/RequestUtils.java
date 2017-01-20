package com.utils;

import android.content.Context;
import android.os.Build;

import com.data.objects.Constants;
import com.model.request.Request;

/**
 * Created by Mor on 12/26/2016.
 */

public abstract class RequestUtils {
    public static void prepareDefaultRequest(Context context, Request request) {
        request.setMessageInitiaterId(Constants.MY_ID(context));
        request.setAppVersion(String.valueOf(Constants.APP_VERSION()));
        request.setPushToken(Constants.MY_BATCH_TOKEN(context));
        request.setAndroidVersion(Build.VERSION.RELEASE);
        request.setDeviceModel(Constants.MY_DEVICE_MODEL());
    }
}
