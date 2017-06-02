package com.validate.media;


import com.utils.MediaFilesUtilsImpl;

/**
 * Created by Mor on 25/08/2016.
 */
public class ValidateVideoFormatBehavior implements ValidateMediaFormatBehavior {
    @Override
    public boolean isValidFormatByLink(String link) {
        return MediaFilesUtilsImpl.isValidVideoFormat(link);
    }
}
