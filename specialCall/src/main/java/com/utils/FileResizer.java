package com.utils;

import android.content.Context;

import FilesManager.FileManager;

/**
 * Created by mor on 12/11/2015.
 */
public class FileResizer {

    private static final int AUDIO_VIDEO_SIZE_COMPRESS_NEEDED = 5242880; //5MB
    private static final int IMAGE_SIZE_COMPRESS_NEEDED = 1048576; //1MB
    private static final long ONE_MINUTE = 60;
    private static final double PERCENT_TO_TRIM = 0.7;

    /**
     * If necessary, reduces file size using various methods and returns new reduced file. Otherwise, returns original (untouched) file.
     * @param baseFile - The file to reduce its size if necessary
     * @param outPath - The path to saved the reduced file in
     * @param context
     * @return
     */
    public static FileManager resizeFileIfNecessary(FileManager baseFile, String outPath, Context context) {

        FileManager modifiedFile = baseFile;
        switch(baseFile.getFileType()) {

            case RINGTONE:
            case VIDEO:

                    if(baseFile.getFileSize() <= AUDIO_VIDEO_SIZE_COMPRESS_NEEDED)
                        return baseFile;

                    Double duration = (double) FFMPEG_Utils.getFileDurationInSeconds(baseFile, context);
                    // If media file is longer than 1 min start trimming procedure
                    if(duration >= ONE_MINUTE) {
                        modifiedFile = FFMPEG_Utils.trim(baseFile, outPath, PERCENT_TO_TRIM * duration, context);
                        duration = (double) FFMPEG_Utils.getFileDurationInSeconds(modifiedFile, context);

                        while (duration >= ONE_MINUTE && modifiedFile.getFileSize() > AUDIO_VIDEO_SIZE_COMPRESS_NEEDED) {
                            modifiedFile = FFMPEG_Utils.trim(modifiedFile, outPath, PERCENT_TO_TRIM * duration, context);
                            duration = (double) FFMPEG_Utils.getFileDurationInSeconds(modifiedFile, context);
                        }
                    }
                    if (modifiedFile.getFileSize() >= AUDIO_VIDEO_SIZE_COMPRESS_NEEDED)
                        modifiedFile = FFMPEG_Utils.compressFile(modifiedFile, outPath, context);

                    modifiedFile.set_uncompdFileFullPath(baseFile.getFileFullPath());
                    modifiedFile.setIsCompressed(true);
                    return modifiedFile;
            case IMAGE:
                    if(baseFile.getFileSize() <= IMAGE_SIZE_COMPRESS_NEEDED)
                       return baseFile;

                    FFMPEG_Utils.getImageResolution(baseFile, context);

        }
        return baseFile;
    }
}
