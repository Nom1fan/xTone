package com_international.utils;

import android.content.Context;

/**
 * Created by Mor on 22/05/2017.
 */

public interface Phone2MediaPathMapperUtils extends Utility {

    // Caller media keys
    String CALLER_VISUAL_MEDIA = "CALLER_VISUAL_MEDIA";
    String DEFAULT_CALLER_VISUAL_MEDIA = "DEFAULT_CALLER_VISUAL_MEDIA";
    String CALLER_AUDIO_MEDIA = "CALLER_AUDIO_MEDIA";
    String DEFAULT_CALLER_AUDIO_MEDIA = "DEFAULT_CALLER_AUDIO_MEDIA";

    // Profile media keys
    String PROFILE_VISUAL_MEDIA = "PROFILE_VISUAL_MEDIA";
    String DEFAULT_PROFILE_VISUAL_MEDIA = "DEFAULT_PROFILE_VISUAL_MEDIA";
    String PROFILE_AUDIO_MEDIA = "PROFILE_AUDIO_MEDIA";
    String DEFAULT_PROFILE_AUDIO_MEDIA = "DEFAULT_PROFILE_AUDIO_MEDIA";


    // Caller media methods
    String getCallerVisualMediaPath(Context context, String phoneNumber);

    void setCallerVisualMediaPath(Context context, String phoneNumber, String mediaPath);

    String getDefaultCallerVisualMediaPath(Context context, String phoneNumber);

    void setDefaultCallerVisualMediaPath(Context context, String phoneNumber, String mediaPath);

    String getCallerAudioMediaPath(Context context, String phoneNumber);

    void setCallerAudioMediaPath(Context context, String phoneNumber, String mediaPath);

    String getDefaultCallerAudioMediaPath(Context context, String phoneNumber);

    void setDefaultCallerAudioMediaPath(Context context, String phoneNumber, String mediaPath);

    void removeCallerVisualMediaPath(Context context, String phoneNumber);

    void removeDefaultCallerVisualMediaPath(Context context, String phoneNumber);

    void removeCallerAudioMediaPath(Context context, String phoneNumber);

    void removeDefaultCallerAudioMediaPath(Context context, String phoneNumber);


    // Profile media methods
    String getProfileVisualMediaPath(Context context, String phoneNumber);

    void setProfileAudioMediaPath(Context context, String phoneNumber, String mediaPath);

    void setProfileVisualMediaPath(Context context, String phoneNumber, String mediaPath);

    String getDefaultProfileVisualMediaPath(Context context, String phoneNumber);

    void setDefaultProfileVisualMediaPath(Context context, String phoneNumber, String mediaPath);

    void setDefaultProfileAudioMediaPath(Context context, String phoneNumber, String mediaPath);

    String getProfileAudioMediaPath(Context context, String phoneNumber);

    String getDefaultProfileAudioMediaPath(Context context, String phoneNumber);

    void removeProfileVisualMediaPath(Context context, String phoneNumber);

    void removeDefaultProfileVisualMediaPath(Context context, String phoneNumber);

    void removeProfileAudioMediaPath(Context context, String phoneNumber);

    void removeDefaultProfileAudioMediaPath(Context context, String phoneNumber);










}
