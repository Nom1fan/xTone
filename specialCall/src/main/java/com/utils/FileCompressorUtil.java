package com.utils;

import android.content.Context;

import com.data_objects.Constants;
import com.netcompss.ffmpeg4android.GeneralUtils;

import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;

/**
 * Created by mor on 12/11/2015.
 */
public abstract class FileCompressorUtil {

    private static final int VIDEO_SIZE_COMPRESS_NEEDED     =   5242880; //5MB
    private static final int AUDIO_SIZE_COMPRESS_NEEDED     =   5242880; //5MB
    private static final int IMAGE_SIZE_COMPRESS_NEEDED     =   1048576; //1MB
    private static final int MIN_RESOLUTION                 =   320;
    private static final long MAX_DURATION                  =   40;      // seconds
    private static final double PERCENT_TO_TRIM             =   0.7;
    private static final String workFolder                  =   Constants.TEMP_COMPRESSED_FOLDER;
    private static final String TAG                         =   FileCompressorUtil.class.getSimpleName();

    /**
     * Compresses all file formats
     *
     * @param baseFile - The file to reduce its size if necessary
     * @param outPath  - The path to saved the reduced file in
     * @param context
     * @return The compressed file, if needed (and possible). Otherwise, the base file.
     */
    public static FileManager compressFileIfNecessary(FileManager baseFile, String outPath, Context context) {

        if (GeneralUtils.isLicenseValid(context, workFolder) < 0)
            return baseFile;

        switch (baseFile.getFileType()) {

            case VIDEO:
                return compressVideo(baseFile, outPath, context);
            case AUDIO:
                return compressAudio(baseFile, outPath, context);
            case IMAGE:
                return compressImage(baseFile, outPath, context);
        }
        return baseFile;
    }

    private static FileManager compressVideo(FileManager baseFile, String outPath, Context context) {

        FileManager modifiedFile;
        if (baseFile.getFileSize() <= VIDEO_SIZE_COMPRESS_NEEDED)
            return baseFile;

        BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.COMPRESSING, null, null));

        modifiedFile = trimMediaFile(baseFile, outPath, VIDEO_SIZE_COMPRESS_NEEDED, context);

        int[] res = FFMPEG_Utils.getVideoResolution(baseFile);
        int width = res[0];
        int height = res[1];

        while (width > MIN_RESOLUTION && modifiedFile.getFileSize() >= VIDEO_SIZE_COMPRESS_NEEDED) {
            modifiedFile = FFMPEG_Utils.compressVideoFile(modifiedFile, outPath, width, height, context);
            res = FFMPEG_Utils.getVideoResolution(modifiedFile);
            width = res[0];
            height = res[1];
        }

        modifiedFile.set_uncompdFileFullPath(baseFile.getFileFullPath());
        modifiedFile.setIsCompressed(true);
        BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.COMPRESSION_COMPLETE, null, null));
        return modifiedFile;
    }

    private static FileManager compressImage(FileManager baseFile, String outPath, Context context) {

        FileManager modifiedFile = baseFile;
        if (baseFile.getFileSize() <= IMAGE_SIZE_COMPRESS_NEEDED)
            return baseFile;

        BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.COMPRESSING, null, null));

        int width = FFMPEG_Utils.getImageResolution(baseFile)[0];

        // If image resolution is larger than MIN_RESOLUTION we start lowering resolution
        while (width > MIN_RESOLUTION && modifiedFile.getFileSize() > IMAGE_SIZE_COMPRESS_NEEDED) {

            modifiedFile = FFMPEG_Utils.compressImageFile(baseFile, outPath, width, context);
            width = FFMPEG_Utils.getImageResolution(modifiedFile)[0];
        }

        modifiedFile.set_uncompdFileFullPath(baseFile.getFileFullPath());
        modifiedFile.setIsCompressed(true);
        BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.REFRESH_UI, null, null));
        return modifiedFile;
    }

    private static FileManager compressAudio(FileManager baseFile, String outPath, Context context) {

        FileManager modifiedFile;
        if (baseFile.getFileSize() <= AUDIO_SIZE_COMPRESS_NEEDED)
            return baseFile;

        BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.COMPRESSING, null, null));

        modifiedFile = trimMediaFile(baseFile, outPath, AUDIO_SIZE_COMPRESS_NEEDED, context);

        //if (modifiedFile.getFileSize() >= AUDIO_SIZE_COMPRESS_NEEDED)
        // TODO See if there is a way to compress non-wav audio files

        modifiedFile.set_uncompdFileFullPath(baseFile.getFileFullPath());
        modifiedFile.setIsCompressed(true);
        BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.REFRESH_UI, null, null));
        return modifiedFile;

    }

    private static FileManager trimMediaFile(FileManager baseFile, String outPath, int sizeToCompress, Context context) {

        FileManager modifiedFile = baseFile;
        Double duration = (double) FFMPEG_Utils.getFileDurationInSeconds(baseFile, context);
        // If media file is longer than 1 min start trimming procedure
        if (duration >= MAX_DURATION) {
            modifiedFile = FFMPEG_Utils.trim(baseFile, outPath, PERCENT_TO_TRIM * duration, context);
            duration = (double) FFMPEG_Utils.getFileDurationInSeconds(modifiedFile, context);

            while (duration >= MAX_DURATION && modifiedFile.getFileSize() > sizeToCompress) {
                modifiedFile = FFMPEG_Utils.trim(modifiedFile, outPath, PERCENT_TO_TRIM * duration, context);
                duration = (double) FFMPEG_Utils.getFileDurationInSeconds(modifiedFile, context);
            }
        }

        return modifiedFile;
    }
}
