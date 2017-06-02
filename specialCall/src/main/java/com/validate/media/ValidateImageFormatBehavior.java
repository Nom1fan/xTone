package com.validate.media;


/**
 * Created by Mor on 25/08/2016.
 */
public class ValidateImageFormatBehavior extends BaseValidateFormatBehavior {
    @Override
    public boolean isValidFormatByLink(String link) {
        return mediaFileUtils.isValidImageFormat(link);
    }
}
