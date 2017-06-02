package com.validate.media;


import com.utils.MediaFilesUtilsImpl;

/**
 * Created by Mor on 26/08/2016.
 */
public class ValidateAudioFormatBehavior implements ValidateMediaFormatBehavior {
    @Override
    public boolean isValidFormatByLink(String link) {
        return MediaFilesUtilsImpl.isValidAudioFormat(link);
    }
}
