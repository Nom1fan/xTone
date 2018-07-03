package com.utils;

import android.content.Context;

import com.logger.Logger;
import com.logger.LoggerFactory;

/**
 * Created by Mor on 10/06/2017.
 */

public class Phone2MediaPathMapperUtilsImpl implements Phone2MediaPathMapperUtils {

    private static final String TAG = PowerManagerUtilsImpl.class.getSimpleName();

    private static Logger logger = LoggerFactory.getLogger();

    // Caller media methods
    //region Getters
    @Override
    public String getCallerVisualMediaPath(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, CALLER_VISUAL_MEDIA, phoneNumber);
    }

    @Override
    public String getCallerAudioMediaPath(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, CALLER_AUDIO_MEDIA, phoneNumber);
    }

    @Override
    public String getDefaultCallerVisualMediaPath(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, DEFAULT_CALLER_VISUAL_MEDIA, phoneNumber);
    }

    @Override
    public String getDefaultCallerAudioMediaPath(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, DEFAULT_CALLER_AUDIO_MEDIA, phoneNumber);
    }
    //endregion

    //region Setters
    @Override
    public void setCallerVisualMediaPath(Context context, String phoneNumber, String mediaPath) {
        logger.debug(TAG, "Setting caller visual media path for:[" + phoneNumber + "] Media path:[" + mediaPath + "]");
        SharedPrefUtils.setString(context, CALLER_VISUAL_MEDIA, phoneNumber, mediaPath);
    }

    @Override
    public void setCallerAudioMediaPath(Context context, String phoneNumber, String mediaPath) {
        logger.debug(TAG, "Setting caller audio media path for:[" + phoneNumber + "] Media path:[" + mediaPath + "]");
        SharedPrefUtils.setString(context, CALLER_AUDIO_MEDIA, phoneNumber, mediaPath);
    }

    @Override
    public void setDefaultCallerVisualMediaPath(Context context, String phoneNumber, String mediaPath) {
        logger.debug(TAG, "Setting default caller visual media path for:[" + phoneNumber + "] Media path:[" + mediaPath + "]");
        SharedPrefUtils.setString(context, DEFAULT_CALLER_VISUAL_MEDIA, phoneNumber, mediaPath);
    }

    @Override
    public void setDefaultCallerAudioMediaPath(Context context, String phoneNumber, String mediaPath) {
        logger.debug(TAG, "Setting caller audio media path for:[" + phoneNumber + "] Media path:[" + mediaPath + "]");
        SharedPrefUtils.setString(context, DEFAULT_CALLER_AUDIO_MEDIA, phoneNumber, mediaPath);
    }
    //endregion

    //region Removers
    @Override
    public void removeCallerVisualMediaPath(Context context, String phoneNumber) {
        logger.debug(TAG, "Removing caller visual media path for:[" + phoneNumber + "]");
        SharedPrefUtils.remove(context, CALLER_VISUAL_MEDIA, phoneNumber);
    }

    @Override
    public void removeCallerAudioMediaPath(Context context, String phoneNumber) {
        logger.debug(TAG, "Removing caller audio media path for:[" + phoneNumber + "]");
        SharedPrefUtils.remove(context, CALLER_AUDIO_MEDIA, phoneNumber);
    }

    @Override
    public void removeDefaultCallerVisualMediaPath(Context context, String phoneNumber) {
        logger.debug(TAG, "Removing default caller visual media path for:[" + phoneNumber + "]");
        SharedPrefUtils.remove(context, DEFAULT_CALLER_VISUAL_MEDIA, phoneNumber);
    }

    @Override
    public void removeDefaultCallerAudioMediaPath(Context context, String phoneNumber) {
        logger.debug(TAG, "Removing default caller audio media path for:[" + phoneNumber + "]");
        SharedPrefUtils.remove(context, DEFAULT_CALLER_AUDIO_MEDIA, phoneNumber);
    }
    //endregion

    // Profile media methods
    //region Getters
    @Override
    public String getProfileVisualMediaPath(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, PROFILE_VISUAL_MEDIA, phoneNumber);
    }

    @Override
    public String getProfileAudioMediaPath(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, PROFILE_AUDIO_MEDIA, phoneNumber);
    }

    @Override
    public String getDefaultProfileVisualMediaPath(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, DEFAULT_PROFILE_VISUAL_MEDIA, phoneNumber);
    }

    @Override
    public String getDefaultProfileAudioMediaPath(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, DEFAULT_PROFILE_AUDIO_MEDIA, phoneNumber);
    }
    //endregion

    //region Setters
    @Override
    public void setProfileVisualMediaPath(Context context, String phoneNumber, String mediaPath) {
        logger.debug(TAG, "Setting profile visual media path for:[" + phoneNumber + "] Media path:[" + mediaPath + "]");
        SharedPrefUtils.setString(context, PROFILE_VISUAL_MEDIA, phoneNumber, mediaPath);
    }

    @Override
    public void setProfileAudioMediaPath(Context context, String phoneNumber, String mediaPath) {
        logger.debug(TAG, "Setting profile audio media path for:[" + phoneNumber + "] Media path:[" + mediaPath + "]");
        SharedPrefUtils.setString(context, PROFILE_AUDIO_MEDIA, phoneNumber, mediaPath);
    }

    @Override
    public void setDefaultProfileVisualMediaPath(Context context, String phoneNumber, String mediaPath) {
        logger.debug(TAG, "Setting default profile visual media path for:[" + phoneNumber + "] Media path:[" + mediaPath + "]");
        SharedPrefUtils.setString(context, DEFAULT_PROFILE_VISUAL_MEDIA, phoneNumber, mediaPath);
    }

    @Override
    public void setDefaultProfileAudioMediaPath(Context context, String phoneNumber, String mediaPath) {
        logger.debug(TAG, "Setting default profile audio media path for:[" + phoneNumber + "] Media path:[" + mediaPath + "]");
        SharedPrefUtils.setString(context, DEFAULT_PROFILE_AUDIO_MEDIA, phoneNumber, mediaPath);
    }
    //endregion

    //region Removers
    @Override
    public void removeProfileVisualMediaPath(Context context, String phoneNumber) {
        logger.debug(TAG, "Removing profile visual media path for:[" + phoneNumber + "]");
        SharedPrefUtils.remove(context, PROFILE_VISUAL_MEDIA, phoneNumber);
    }

    @Override
    public void removeProfileAudioMediaPath(Context context, String phoneNumber) {
        logger.debug(TAG, "Removing profile audio media path for:[" + phoneNumber + "]");
        SharedPrefUtils.remove(context, PROFILE_AUDIO_MEDIA, phoneNumber);
    }

    @Override
    public void removeDefaultProfileVisualMediaPath(Context context, String phoneNumber) {
        logger.debug(TAG, "Removing default profile visual media path for:[" + phoneNumber + "]");
        SharedPrefUtils.remove(context, DEFAULT_PROFILE_VISUAL_MEDIA, phoneNumber);
    }

    @Override
    public void removeDefaultProfileAudioMediaPath(Context context, String phoneNumber) {
        logger.debug(TAG, "Removing default profile audio media path for:[" + phoneNumber + "]");
        SharedPrefUtils.remove(context, DEFAULT_PROFILE_AUDIO_MEDIA, phoneNumber);
    }
    //endregion

    // Uploaded media methods
    //region Getters
    @Override
    public String getUploadedCallerVisualMediaPath(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, UPLOADED_CALLER_MEDIA_THUMBNAIL, phoneNumber);
    }

    @Override
    public String getUploadedProfileVisualMediaPath(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, UPLOADED_PROFILE_MEDIA_THUMBNAIL, phoneNumber);
    }

    @Override
    public String getUploadedFuntoneMediaPath(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, UPLOADED_FUNTONE_PATH, phoneNumber);
    }

    @Override
    public String getUploadedRingtoneMediaPath(Context context, String phoneNumber) {
        return SharedPrefUtils.getString(context, UPLOADED_RINGTONE_PATH, phoneNumber);
    }
    //endregion

    //region Setters
    @Override
    public void setUploadedCallerVisualMediaPath(Context context, String phoneNumber, String mediaPath) {
        SharedPrefUtils.setString(context, UPLOADED_CALLER_MEDIA_THUMBNAIL, phoneNumber, mediaPath);
    }

    @Override
    public void setUploadedProfileVisualMediaPath(Context context, String phoneNumber, String mediaPath) {
        SharedPrefUtils.setString(context, UPLOADED_PROFILE_MEDIA_THUMBNAIL, phoneNumber, mediaPath);
    }

    @Override
    public void setUploadedFuntoneMediaPath(Context context, String phoneNumber, String mediaPath) {
        SharedPrefUtils.setString(context, UPLOADED_FUNTONE_PATH, phoneNumber, mediaPath);
    }

    @Override
    public void setUploadedRingtoneMediaPath(Context context, String phoneNumber, String mediaPath) {
        SharedPrefUtils.setString(context, UPLOADED_RINGTONE_PATH, phoneNumber, mediaPath);
    }
    //endregion

    //region Removers
    @Override
    public void removeUploadedCallerVisualMediaPath(Context context, String phoneNumber) {
        SharedPrefUtils.remove(context, UPLOADED_CALLER_MEDIA_THUMBNAIL, phoneNumber);
    }

    @Override
    public void removeUploadedProfileVisualMediaPath(Context context, String phoneNumber) {
        SharedPrefUtils.remove(context, UPLOADED_PROFILE_MEDIA_THUMBNAIL, phoneNumber);
    }

    @Override
    public void removeUploadedFuntoneMediaPath(Context context, String phoneNumber) {
        SharedPrefUtils.remove(context, UPLOADED_FUNTONE_PATH, phoneNumber);
    }

    @Override
    public void removeUploadedRingtoneMediaPath(Context context, String phoneNumber) {
        SharedPrefUtils.remove(context, UPLOADED_RINGTONE_PATH, phoneNumber);
    }
    //endregion
}
