package com_international.validate.media;


/**
 * Created by Mor on 26/08/2016.
 */
public class ValidateAudioFormatBehavior extends BaseValidateFormatBehavior {

    @Override
    public boolean isValidFormatByLink(String link) {
        return mediaFileUtils.isValidAudioFormat(link);
    }
}
