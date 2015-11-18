package com.utils;

import android.content.Context;

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

    public LUT_Utils(Context context) {
        _context = context;
    }

    public void saveUploadedMediaPerNumber(String destPhoneNumber, String mediaPath) {
        SharedPrefUtils.setString(_context, SharedPrefUtils.UPLOADED_MEDIA_THUMBNAIL, destPhoneNumber, mediaPath);
    }

    public String getUploadedMediaPerNumber(String destPhoneNumber) {
        return SharedPrefUtils.getString(_context, SharedPrefUtils.UPLOADED_MEDIA_THUMBNAIL, destPhoneNumber);
    }

    public void removeUploadedMediaPerNumber(String destPhoneNumber) {
        SharedPrefUtils.remove(_context, SharedPrefUtils.UPLOADED_MEDIA_THUMBNAIL, destPhoneNumber);
    }

    public void saveUploadedRingTonePerNumber(String destPhoneNumber, String mediaPath) {
        SharedPrefUtils.setString(_context, SharedPrefUtils.WAS_RINGTONE_UPLOADED, destPhoneNumber, mediaPath);
    }

    public String getUploadedRingTonePerNumber(String destPhoneNumber) {
        return SharedPrefUtils.getString(_context, SharedPrefUtils.WAS_RINGTONE_UPLOADED, destPhoneNumber);
    }

    public void removeUploadedRingTonePerNumber(String destPhoneNumber) {
        SharedPrefUtils.remove(_context, SharedPrefUtils.WAS_RINGTONE_UPLOADED, destPhoneNumber);
    }

    public void saveUploadedPerNumber(String destPhoneNumber, FileManager.FileType fileType, String mediaPath) {

        switch (fileType) {
            case IMAGE:
                saveUploadedMediaPerNumber(destPhoneNumber, mediaPath);
                break;
            case VIDEO:
                saveUploadedMediaPerNumber(destPhoneNumber, mediaPath);
                removeUploadedRingTonePerNumber(destPhoneNumber);
                break;
            case RINGTONE:
                saveUploadedRingTonePerNumber(destPhoneNumber, mediaPath);

                // Checking if video was marked as last uploaded, if so need to delete (ringtone cannot co-exist with video)
                String thumbPath = getUploadedMediaPerNumber(destPhoneNumber);
                try {
                    FileManager.FileType prevType = FileManager.getFileType(thumbPath);
                    if (prevType == FileManager.FileType.VIDEO)
                        removeUploadedMediaPerNumber(destPhoneNumber);

                } catch (FileInvalidFormatException | FileDoesNotExistException | FileMissingExtensionException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
