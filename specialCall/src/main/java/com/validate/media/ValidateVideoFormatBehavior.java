package com.validate.media;


import com.utils.MediaFilesUtils;

/**
 * Created by Mor on 25/08/2016.
 */
public class ValidateVideoFormatBehavior implements ValidateMediaFormatBehavior {
    @Override
    public boolean isValidFormatByLink(String link) {
        return MediaFilesUtils.isValidVideoFormat(link);
    }
}
