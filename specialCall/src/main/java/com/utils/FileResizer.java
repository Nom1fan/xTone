package com.utils;

import android.content.Context;

import FilesManager.FileManager;

/**
 * Created by mor on 12/11/2015.
 */
public class FileResizer {

    private static final int SIZE_COMPRESS_NEEDED = 5242880;
    private static final long ONE_MINUTE = 60;

    /**
     * If necessary, reduces file size using various methods and returns new reduced file. Otherwise, returns original (untouched) file.
     * @param managedFile - The file to reduce its size if necessary
     * @param outPath - The path to saved the reduced file in
     * @param context
     * @return
     */
    public static FileManager resizeFileIfNecessary(FileManager managedFile, String outPath, Context context) {

        if(managedFile.getFileSize() <= SIZE_COMPRESS_NEEDED)
            return managedFile;

        FileManager modifiedFile = managedFile;
        switch(managedFile.getFileType()) {

            case RINGTONE:
            case VIDEO:

                    Double duration = (double) FFMPEG_Utils.getFileDurationInSeconds(managedFile, context);
                    // If media file is longer than 1 min start trimming procedure
                    if(duration >= ONE_MINUTE) {
                        modifiedFile = FFMPEG_Utils.trim(managedFile, outPath, 0.7 * duration, context);
                        duration = (double) FFMPEG_Utils.getFileDurationInSeconds(modifiedFile, context);

                        while (duration >= ONE_MINUTE && modifiedFile.getFileSize() > SIZE_COMPRESS_NEEDED) {
                            modifiedFile = FFMPEG_Utils.trim(modifiedFile, outPath, 0.7 * duration, context);
                            duration = (double) FFMPEG_Utils.getFileDurationInSeconds(modifiedFile, context);
                        }
                    }
                    if (modifiedFile.getFileSize() >= SIZE_COMPRESS_NEEDED)
                        modifiedFile = FFMPEG_Utils.compressFile(modifiedFile, modifiedFile.getFileFullPath(), context);
                    return modifiedFile;
            case IMAGE:
                //TODO: Implement image compression

        }
        return managedFile;
    }
}
