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

    private static final int VIDEO_SIZE_COMPRESS_NEEDED = 5242880; //5MB
    private static final int AUDIO_SIZE_COMPRESS_NEEDED = 5242880; //5MB
    private static final int IMAGE_SIZE_COMPRESS_NEEDED = 1048576; //1MB
    private static final int IMAGE_MIN_RESOLUTION = 320;
    private static final long ONE_MINUTE = 60;
    private static final double PERCENT_TO_TRIM = 0.7;
    private static final String tag = FileCompressorUtil.class.getSimpleName();
    private static final String workFolder = Constants.TEMP_COMPRESSED_FOLDER;
    private static final String TAG = FileCompressorUtil.class.getSimpleName();

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

        FileManager modifiedFile = baseFile;
        switch (baseFile.getFileType()) {

            case VIDEO:
                if (baseFile.getFileSize() <= VIDEO_SIZE_COMPRESS_NEEDED)
                    return baseFile;

                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.COMPRESSING, "Compressing file...", null));

                modifiedFile = trimMediaFile(baseFile, outPath, VIDEO_SIZE_COMPRESS_NEEDED, context);

                if (modifiedFile.getFileSize() >= VIDEO_SIZE_COMPRESS_NEEDED)
                    modifiedFile = FFMPEG_Utils.compressVideoFile(modifiedFile, outPath, context);

                modifiedFile.set_uncompdFileFullPath(baseFile.getFileFullPath());
                modifiedFile.setIsCompressed(true);
                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.COMPRESSION_COMPLETE, "Compression complete.", null));
                return modifiedFile;
            case RINGTONE:
                if (baseFile.getFileSize() <= AUDIO_SIZE_COMPRESS_NEEDED)
                    return baseFile;

                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.COMPRESSING, "Compressing file...", null));

                modifiedFile = trimMediaFile(baseFile, outPath, AUDIO_SIZE_COMPRESS_NEEDED, context);

                //if (modifiedFile.getFileSize() >= AUDIO_SIZE_COMPRESS_NEEDED)
                // TODO See if there is a way to compress non-wav audio files

                modifiedFile.set_uncompdFileFullPath(baseFile.getFileFullPath());
                modifiedFile.setIsCompressed(true);
                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.REFRESH_UI, "Compression complete.", null));
                return modifiedFile;
            case IMAGE:
                if (baseFile.getFileSize() <= IMAGE_SIZE_COMPRESS_NEEDED)
                    return baseFile;

                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.COMPRESSING, "Compressing file...", null));

                int width = FFMPEG_Utils.getImageResolution(baseFile.getFileFullPath())[0];

                // If image resolution is larger than IMAGE_MIN_RESOLUTION we start lowering resolution
                while (width > IMAGE_MIN_RESOLUTION && modifiedFile.getFileSize() > IMAGE_SIZE_COMPRESS_NEEDED) {

                    modifiedFile = FFMPEG_Utils.compressImageFile(baseFile, outPath, width, context);
                    width = FFMPEG_Utils.getImageResolution(modifiedFile.getFileFullPath())[0];
                }

                modifiedFile.set_uncompdFileFullPath(baseFile.getFileFullPath());
                modifiedFile.setIsCompressed(true);
                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.REFRESH_UI, "Compression complete.", null));
                return modifiedFile;
        }
        return baseFile;
    }

    private static FileManager trimMediaFile(FileManager baseFile, String outPath, int sizeToCompress, Context context) {

        FileManager modifiedFile = baseFile;
        Double duration = (double) FFMPEG_Utils.getFileDurationInSeconds(baseFile, context);
        // If media file is longer than 1 min start trimming procedure
        if (duration >= ONE_MINUTE) {
            modifiedFile = FFMPEG_Utils.trim(baseFile, outPath, PERCENT_TO_TRIM * duration, context);
            duration = (double) FFMPEG_Utils.getFileDurationInSeconds(modifiedFile, context);

            while (duration >= ONE_MINUTE && modifiedFile.getFileSize() > sizeToCompress) {
                modifiedFile = FFMPEG_Utils.trim(modifiedFile, outPath, PERCENT_TO_TRIM * duration, context);
                duration = (double) FFMPEG_Utils.getFileDurationInSeconds(modifiedFile, context);
            }
        }

        return modifiedFile;
    }
}
