package com.utils;

import android.content.Context;
import android.util.Log;

import com.enums.SpecialMediaType;
import com.files.media.MediaFile;

import java.io.File;

/**
 * Last Uploads Thumbnails Utilities. Manages the last upload that was made per user for each special media type using SharedPreferences
 *
 * @author mor
 */
public class LUT_Utils {

    private static String TAG = LUT_Utils.class.getSimpleName();

    private String _SharedPrefKeyForVisualMedia;
    private String _SharedPrefKeyForAudioMedia;
    private MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);

    public LUT_Utils(SpecialMediaType specialMediaType) {

        Log.d(TAG, "Constructing with specialMediaType=" + specialMediaType);
        if (specialMediaType == SpecialMediaType.CALLER_MEDIA) {

            _SharedPrefKeyForVisualMedia = SharedPrefUtils.UPLOADED_CALLER_MEDIA_THUMBNAIL;
            _SharedPrefKeyForAudioMedia = SharedPrefUtils.UPLOADED_RINGTONE_PATH;
        } else if (specialMediaType == SpecialMediaType.PROFILE_MEDIA) {

            _SharedPrefKeyForVisualMedia = SharedPrefUtils.UPLOADED_PROFILE_MEDIA_THUMBNAIL;
            _SharedPrefKeyForAudioMedia = SharedPrefUtils.UPLOADED_FUNTONE_PATH;
        } else if(specialMediaType == SpecialMediaType.DEFAULT_CALLER_MEDIA) {
            _SharedPrefKeyForVisualMedia = Phone2MediaPathMapperUtils.DEFAULT_CALLER_VISUAL_MEDIA;
            _SharedPrefKeyForAudioMedia = Phone2MediaPathMapperUtils.DEFAULT_CALLER_AUDIO_MEDIA;
        } else if(specialMediaType == SpecialMediaType.DEFAULT_PROFILE_MEDIA) {
            _SharedPrefKeyForVisualMedia = Phone2MediaPathMapperUtils.DEFAULT_PROFILE_VISUAL_MEDIA;
            _SharedPrefKeyForAudioMedia = Phone2MediaPathMapperUtils.DEFAULT_PROFILE_AUDIO_MEDIA;
        }

        Log.d(TAG, "Selected _SharedPrefKeyForVisualMedia=" + _SharedPrefKeyForVisualMedia
                + ", _SharedPrefKeyForAudioMedia=" + _SharedPrefKeyForAudioMedia);

    }

    public void saveUploadedMediaPerNumber(Context context, String destPhoneNumber, String mediaPath) {
        Log.d(TAG, "saveUploadedMediaPerNumber(): _SharedPrefKeyForVisualMedia=" + _SharedPrefKeyForVisualMedia
                + " destPhoneNumber=" + destPhoneNumber.substring(destPhoneNumber.length()-6) + "path=" + mediaPath);
        SharedPrefUtils.setString(context, _SharedPrefKeyForVisualMedia, destPhoneNumber.substring(destPhoneNumber.length()-6), mediaPath);
    }

    public String getUploadedMediaPerNumber(Context context, String destPhoneNumber) {
        Log.d(TAG, "getUploadedMediaPerNumber(): _SharedPrefKeyForVisualMedia=" + _SharedPrefKeyForVisualMedia
                + " destPhoneNumber=" + destPhoneNumber.substring(destPhoneNumber.length()-6));
        return SharedPrefUtils.getString(context, _SharedPrefKeyForVisualMedia, destPhoneNumber.substring(destPhoneNumber.length()-6));
    }

    public void removeUploadedMediaPerNumber(Context context, String destPhoneNumber) {
        Log.d(TAG, "removeUploadedMediaPerNumber(): _SharedPrefKeyForVisualMedia=" + _SharedPrefKeyForVisualMedia
                + " destPhoneNumber=" + destPhoneNumber.substring(destPhoneNumber.length()-6));
        SharedPrefUtils.remove(context, _SharedPrefKeyForVisualMedia, destPhoneNumber.substring(destPhoneNumber.length()-6));
    }

    public void saveUploadedTonePerNumber(Context context, String destPhoneNumber, String mediaPath) {
        Log.d(TAG, "saveUploadedTonePerNumber(): _SharedPrefKeyForAudioMedia=" + _SharedPrefKeyForAudioMedia
                + " destPhoneNumber=" + destPhoneNumber.substring(destPhoneNumber.length()-6) + "path=" + mediaPath);
        SharedPrefUtils.setString(context, _SharedPrefKeyForAudioMedia, destPhoneNumber.substring(destPhoneNumber.length()-6), mediaPath);
    }

    public String getUploadedTonePerNumber(Context context, String destPhoneNumber) {
        Log.d(TAG, "getUploadedTonePerNumber(): _SharedPrefKeyForAudioMedia=" + _SharedPrefKeyForAudioMedia
                + " destPhoneNumber=" + destPhoneNumber.substring(destPhoneNumber.length()-6));
        return SharedPrefUtils.getString(context, _SharedPrefKeyForAudioMedia, destPhoneNumber.substring(destPhoneNumber.length()-6));
    }

    public void removeUploadedTonePerNumber(Context context, String destPhoneNumber) {
        Log.d(TAG, "removeUploadedTonePerNumber(): _SharedPrefKeyForAudioMedia=" + _SharedPrefKeyForAudioMedia
                + " destPhoneNumber=" + destPhoneNumber.substring(destPhoneNumber.length()-6));
        SharedPrefUtils.remove(context, _SharedPrefKeyForAudioMedia, destPhoneNumber.substring(destPhoneNumber.length()-6));
    }

    public void saveUploadedPerNumber(Context context, String phoneNumber, String mediaPath) {

        MediaFile mediaFile = new MediaFile(new File(mediaPath));

        Log.d(TAG, "saveUploadedPerNumber(): phoneNumber=" +
                phoneNumber + ", fileType=" + mediaFile.getFileType() + ", mediaPath=" + mediaPath);

        switch (mediaFile.getFileType()) {
            case IMAGE:
                saveUploadedMediaPerNumber(context, phoneNumber, mediaPath);
                break;
            case VIDEO:
                saveUploadedMediaPerNumber(context, phoneNumber, mediaPath);
                removeUploadedTonePerNumber(context, phoneNumber);
                break;
            case AUDIO:
                saveUploadedTonePerNumber(context, phoneNumber, mediaPath);

                // Checking if video was marked as last uploaded, if so need to delete (ringtone cannot co-exist with video)
                String thumbPath = getUploadedMediaPerNumber(context, phoneNumber);
                if (!thumbPath.equals("")) {

                    MediaFile.FileType prevType = mediaFileUtils.getFileType(thumbPath);
                    if (prevType == MediaFile.FileType.VIDEO) {
                        removeUploadedMediaPerNumber(context, phoneNumber);
                    }
                }
                break;
        }
    }
}
