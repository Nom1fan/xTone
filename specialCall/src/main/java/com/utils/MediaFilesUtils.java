package com.utils;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Mor on 01/07/2016.
 */
public abstract class MediaFilesUtils {

    private static final String[] imageFormats = { "jpg", "png", "jpeg", "bmp", "gif" , "webp" };
    private static final List<String> imageFormatsList = Arrays.asList(imageFormats);
    private static final String[] audioFormats = { "mp3", "ogg" , "flac" , "mid" , "xmf" , "mxmf" , "rtx" , "ota" , "imy" , "wav" ,"m4a" , "aac"};
    private static final List<String> audioFormatsList = Arrays.asList(audioFormats);
    private static final String[] videoFormats = { "avi", "mpeg", "mp4", "3gp", "wmv" , "webm" , "mkv"  };
    private static final List<String> videoFormatsList = Arrays.asList(videoFormats);


}
