package com.utils;

import android.content.Context;

import com.data.objects.Contact;

/**
 * Created by Mor on 22/05/2017.
 */

public abstract class Phone2MediaMapperUtils {

    public static String getCallerVisualMedia(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, SharedPrefUtils.CALLER_MEDIA_FILEPATH, phoneNumber);
    }

    public static void removeCallerVisualMedia(Context context, String phoneNumber) {
        SharedPrefUtils.remove(context, SharedPrefUtils.CALLER_MEDIA_FILEPATH, phoneNumber);
    }

    public static String getCallerAudioMedia(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, SharedPrefUtils.RINGTONE_FILEPATH, phoneNumber);
    }

    public static void removeCallerAudioMedia(Context context, String phoneNumber) {
        SharedPrefUtils.remove(context, SharedPrefUtils.RINGTONE_FILEPATH, phoneNumber);
    }

    public static String getProfileVisualMedia(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, SharedPrefUtils.PROFILE_MEDIA_FILEPATH, phoneNumber);
    }

    public static void removeProfileVisualMedia(Context context, String phoneNumber) {
        SharedPrefUtils.remove(context, SharedPrefUtils.PROFILE_MEDIA_FILEPATH, phoneNumber);
    }

    public static String getProfileAudioMedia(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, SharedPrefUtils.FUNTONE_FILEPATH, phoneNumber);
    }

    public static void removeProfileAudioMedia(Context context, String phoneNumber) {
        SharedPrefUtils.remove(context, SharedPrefUtils.FUNTONE_FILEPATH, phoneNumber);
    }
}
