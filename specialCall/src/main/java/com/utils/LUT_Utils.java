package com.utils;

import android.content.Context;
import android.util.Log;

import com.data.objects.SpecialMediaType;
import com.exceptions.FileDoesNotExistException;
import com.exceptions.FileInvalidFormatException;
import com.exceptions.FileMissingExtensionException;
import com.files.media.MediaFile;

/**
 * Last Uploads Thumbnails Utilities. Manages the last upload that was made per user for each special media type using SharedPreferences
 * @author mor
 */
public class LUT_Utils {

    private String _SharedPrefKeyForVisualMedia;
    private String _SharedPrefKeyForAudioMedia;
    private static String TAG = LUT_Utils.class.getSimpleName();

    public LUT_Utils(SpecialMediaType specialMediaType) {

        Log.d(TAG, "Constructing with specialMediaType="+specialMediaType);
        if(specialMediaType == SpecialMediaType.CALLER_MEDIA) {

            _SharedPrefKeyForVisualMedia = SharedPrefUtils.UPLOADED_CALLER_MEDIA_THUMBNAIL;
            _SharedPrefKeyForAudioMedia = SharedPrefUtils.UPLOADED_RINGTONE_PATH;
        }
        else if(specialMediaType == SpecialMediaType.PROFILE_MEDIA) {

            _SharedPrefKeyForVisualMedia = SharedPrefUtils.UPLOADED_PROFILE_MEDIA_THUMBNAIL;
            _SharedPrefKeyForAudioMedia = SharedPrefUtils.UPLOADED_FUNTONE_PATH;
        }

        Log.d(TAG, "Selected _SharedPrefKeyForVisualMedia="+_SharedPrefKeyForVisualMedia
                + ", _SharedPrefKeyForAudioMedia=" + _SharedPrefKeyForAudioMedia );

    }

    public void saveUploadedMediaPerNumber(Context context, String destPhoneNumber, String mediaPath) {
        Log.d(TAG, "saveUploadedMediaPerNumber(): _SharedPrefKeyForVisualMedia="+_SharedPrefKeyForVisualMedia
                +" destPhoneNumber="+destPhoneNumber+ "path="+mediaPath);
        SharedPrefUtils.setString(context, _SharedPrefKeyForVisualMedia, destPhoneNumber, mediaPath);
    }

    public String getUploadedMediaPerNumber(Context context, String destPhoneNumber) {
        Log.d(TAG, "getUploadedMediaPerNumber(): _SharedPrefKeyForVisualMedia="+_SharedPrefKeyForVisualMedia
                +" destPhoneNumber="+destPhoneNumber);
        return SharedPrefUtils.getString(context, _SharedPrefKeyForVisualMedia, destPhoneNumber);
    }

    public void removeUploadedMediaPerNumber(Context context, String destPhoneNumber) {
        Log.d(TAG, "removeUploadedMediaPerNumber(): _SharedPrefKeyForVisualMedia="+_SharedPrefKeyForVisualMedia
                +" destPhoneNumber="+destPhoneNumber);
        SharedPrefUtils.remove(context, _SharedPrefKeyForVisualMedia, destPhoneNumber);
    }

    public void saveUploadedTonePerNumber(Context context, String destPhoneNumber, String mediaPath) {
        Log.d(TAG, "saveUploadedTonePerNumber(): _SharedPrefKeyForAudioMedia="+_SharedPrefKeyForAudioMedia
                +" destPhoneNumber="+destPhoneNumber+ "path="+mediaPath);
        SharedPrefUtils.setString(context, _SharedPrefKeyForAudioMedia, destPhoneNumber, mediaPath);
    }

    public String getUploadedTonePerNumber(Context context, String destPhoneNumber) {
        Log.d(TAG, "getUploadedTonePerNumber(): _SharedPrefKeyForAudioMedia="+_SharedPrefKeyForAudioMedia
                +" destPhoneNumber="+destPhoneNumber);
        return SharedPrefUtils.getString(context, _SharedPrefKeyForAudioMedia, destPhoneNumber);
    }

    public void removeUploadedTonePerNumber(Context context, String destPhoneNumber) {
        Log.d(TAG, "removeUploadedTonePerNumber(): _SharedPrefKeyForAudioMedia="+_SharedPrefKeyForAudioMedia
                +" destPhoneNumber="+destPhoneNumber);
        SharedPrefUtils.remove(context, _SharedPrefKeyForAudioMedia, destPhoneNumber);
    }

    public void saveUploadedPerNumber(Context context, String destPhoneNumber, MediaFile.FileType fileType, String mediaPath) {

        Log.d(TAG, "saveUploadedPerNumber(): destPhoneNumber=" +
                destPhoneNumber + ", fileType="+fileType +", mediaPath="+mediaPath);

        switch (fileType) {
            case IMAGE:
                saveUploadedMediaPerNumber(context, destPhoneNumber, mediaPath);
                break;
            case VIDEO:
                saveUploadedMediaPerNumber(context, destPhoneNumber, mediaPath);
                removeUploadedTonePerNumber(context, destPhoneNumber);
                break;
            case AUDIO:
                saveUploadedTonePerNumber(context, destPhoneNumber, mediaPath);

                // Checking if video was marked as last uploaded, if so need to delete (ringtone cannot co-exist with video)
                String thumbPath = getUploadedMediaPerNumber(context, destPhoneNumber);
                if(!thumbPath.equals("")) {
                    try {
                        MediaFile.FileType prevType = MediaFile.getFileType(thumbPath);
                        if (prevType == MediaFile.FileType.VIDEO)
                            removeUploadedMediaPerNumber(context, destPhoneNumber);

                    } catch (FileInvalidFormatException | FileDoesNotExistException | FileMissingExtensionException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }
}
