package com.utils;

import android.content.Context;
import android.util.Log;

import DataObjects.SpecialMediaType;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;

/**
 * Last Uploads Thumbnails Manager. Manages the last upload that was made per user using SharedPreferences
 * @author mor
 */
public class LUT_Utils {

    private Context _context;
    private String _SharedPrefKeyForVisualMedia;
    private String _SharedPrefKeyForAudioMedia;
    private static String TAG = LUT_Utils.class.getSimpleName();

    public LUT_Utils(Context context, SpecialMediaType specialMediaType) {

        _context = context;
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

    public void saveUploadedMediaPerNumber(String destPhoneNumber, String mediaPath) {
        Log.d(TAG, "saveUploadedMediaPerNumber(): _SharedPrefKeyForVisualMedia="+_SharedPrefKeyForVisualMedia
                +" destPhoneNumber="+destPhoneNumber+ "path="+mediaPath);
        SharedPrefUtils.setString(_context, _SharedPrefKeyForVisualMedia, destPhoneNumber, mediaPath);
    }

    public String getUploadedMediaPerNumber(String destPhoneNumber) {
        Log.d(TAG, "getUploadedMediaPerNumber(): _SharedPrefKeyForVisualMedia="+_SharedPrefKeyForVisualMedia
                +" destPhoneNumber="+destPhoneNumber);
        return SharedPrefUtils.getString(_context, _SharedPrefKeyForVisualMedia, destPhoneNumber);
    }

    public void removeUploadedMediaPerNumber(String destPhoneNumber) {
        Log.d(TAG, "removeUploadedMediaPerNumber(): _SharedPrefKeyForVisualMedia="+_SharedPrefKeyForVisualMedia
                +" destPhoneNumber="+destPhoneNumber);
        SharedPrefUtils.remove(_context, _SharedPrefKeyForVisualMedia, destPhoneNumber);
    }

    public void saveUploadedTonePerNumber(String destPhoneNumber, String mediaPath) {
        Log.d(TAG, "saveUploadedTonePerNumber(): _SharedPrefKeyForAudioMedia="+_SharedPrefKeyForAudioMedia
                +" destPhoneNumber="+destPhoneNumber+ "path="+mediaPath);
        SharedPrefUtils.setString(_context, _SharedPrefKeyForAudioMedia, destPhoneNumber, mediaPath);
    }

    public String getUploadedTonePerNumber(String destPhoneNumber) {
        Log.d(TAG, "getUploadedTonePerNumber(): _SharedPrefKeyForAudioMedia="+_SharedPrefKeyForAudioMedia
                +" destPhoneNumber="+destPhoneNumber);
        return SharedPrefUtils.getString(_context, _SharedPrefKeyForAudioMedia, destPhoneNumber);
    }

    public void removeUploadedTonePerNumber(String destPhoneNumber) {
        Log.d(TAG, "removeUploadedTonePerNumber(): _SharedPrefKeyForAudioMedia="+_SharedPrefKeyForAudioMedia
                +" destPhoneNumber="+destPhoneNumber);
        SharedPrefUtils.remove(_context, _SharedPrefKeyForAudioMedia, destPhoneNumber);
    }

    public void saveUploadedPerNumber(String destPhoneNumber, FileManager.FileType fileType, String mediaPath) {

        Log.d(TAG, "saveUploadedPerNumber(): destPhoneNumber=" +
                destPhoneNumber + ", fileType="+fileType +", mediaPath="+mediaPath);

        switch (fileType) {
            case IMAGE:
                saveUploadedMediaPerNumber(destPhoneNumber, mediaPath);
                break;
            case VIDEO:
                saveUploadedMediaPerNumber(destPhoneNumber, mediaPath);
                removeUploadedTonePerNumber(destPhoneNumber);
                break;
            case RINGTONE:
                saveUploadedTonePerNumber(destPhoneNumber, mediaPath);

                // Checking if video was marked as last uploaded, if so need to delete (ringtone cannot co-exist with video)
                String thumbPath = getUploadedMediaPerNumber(destPhoneNumber);
                if(!thumbPath.equals("")) {
                    try {
                        FileManager.FileType prevType = FileManager.getFileType(thumbPath);
                        if (prevType == FileManager.FileType.VIDEO)
                            removeUploadedMediaPerNumber(destPhoneNumber);

                    } catch (FileInvalidFormatException | FileDoesNotExistException | FileMissingExtensionException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }
}
