package com.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.data_objects.Constants;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.loader.LoadJNI;

import java.io.FileOutputStream;
import java.io.IOException;

import FilesManager.FileManager;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by mor on 12/11/2015.
 */
public class MediaFileProcessingUtils {


    //region Constants for FFMPEG compression algorithms
    public static final String workFolder = Constants.COMPRESSED_FOLDER;
    public static final String VK_LOG_PATH = workFolder + "vk.log";
    public static final int VIDEO_SIZE_COMPRESS_NEEDED = 3145728; // 3MB
    public static final int AUDIO_SIZE_COMPRESS_NEEDED = 3145728; // 3MB
    public static final int IMAGE_SIZE_COMPRESS_NEEDED = 1048576; // 1MB
    public static final int AFTER_TRIM_SIZE_COMPRESS_NEEDED = 4194304; // 4MB
    public static final long MAX_DURATION = 31;      // seconds
    public static final int MIN_RESOLUTION = 320;     // MIN width resolution
    public static final int FINISHED_TRANSCODING_MSG = 0;
    public static final int COMPRESSION_PHASE_2 = 1;
    private static final String TAG = MediaFileProcessingUtils.class.getSimpleName();
    //endregion

    //region Constants for handler messages
    public static final int STOP_TRANSCODING_MSG = -1;
    //endregion

    private FFMPEG_Utils _ffmpeg_utils;

    public MediaFileProcessingUtils(LoadJNI vk, Handler compressHandler) {
        _ffmpeg_utils = new FFMPEG_Utils(vk, compressHandler);
    }

    public MediaFileProcessingUtils(LoadJNI vk) {
        _ffmpeg_utils = new FFMPEG_Utils(vk);
    }

    public MediaFileProcessingUtils() {

    }


    public FileManager trimMediaFile(FileManager baseFile, String trimmedFilePath ,Context context) {

        if (GeneralUtils.isLicenseValid(context, workFolder) < 0) {
            return baseFile;
        }

        FileManager trimmedFile = null;
        FileManager.FileType type = baseFile.getFileType();

        Log.d(TAG, "Acquire wake lock");
        PowerManager.WakeLock wakeLock = getWakeLock(context);
        wakeLock.acquire();

        if (type.equals(FileManager.FileType.AUDIO))
            trimmedFile = trimAudioAndVideo(baseFile, trimmedFilePath, AUDIO_SIZE_COMPRESS_NEEDED, context);

        if (type.equals(FileManager.FileType.VIDEO))
            trimmedFile = trimAudioAndVideo(baseFile, trimmedFilePath, VIDEO_SIZE_COMPRESS_NEEDED, context);

        releaseWakeLock(wakeLock);

        if (trimmedFile != null) {
            markFilePathAsTrimmed(context, trimmedFile.getFile().getAbsolutePath());

            MediaFilesUtils.triggerMediaScanOnFile(context, trimmedFile.getFile());
            return trimmedFile;
        }

        return baseFile;
    }

    /**
     * Compresses all file formats
     *
     * @param baseFile - The file to reduce its size if necessary
     * @param context Application context
     * @return The compressed file, if necessary (and possible). Otherwise, the base file.
     */
    public FileManager compressMediaFile(FileManager baseFile, String compressedFilePath, Context context) {

        if (GeneralUtils.isLicenseValid(context, workFolder) < 0) {
            return baseFile;
        }

        FileManager compressedFile = null;

        Log.d(TAG, "Acquire wake lock");
        PowerManager.WakeLock wakeLock = getWakeLock(context);
        wakeLock.acquire();

        switch (baseFile.getFileType()) {

            case VIDEO:
                compressedFile = compressVideo(baseFile, compressedFilePath, context);
                break;
            case AUDIO:
                compressedFile = compressAudio(baseFile, compressedFilePath, context);
                break;
            case IMAGE:
                compressedFile = compressImage(baseFile, compressedFilePath, context);
                break;
        }

        releaseWakeLock(wakeLock);

        if(compressedFile!=null) {
            // This means we compressed a trimmed file which was still too large - Deleting to avoid duplicates
            if (isFileTrimmed(context, baseFile)) {
                String trimmedFilePath = baseFile.getFile().getAbsolutePath();
                SharedPrefUtils.remove(context, SharedPrefUtils.TRIMMED_FILES, trimmedFilePath);
                FileManager.delete(baseFile.getFile());
            }

            markFilePathAsCompressed(context, compressedFilePath);
            MediaFilesUtils.triggerMediaScanOnFile(context, compressedFile.getFile());
            return compressedFile;
        }

        return baseFile;
    }

    public FileManager rotateImageFile(FileManager baseFile, String rotatedImageFilepath, Context context) {

        int degrees = SharedPrefUtils.getInt(context, SharedPrefUtils.GENERAL, SharedPrefUtils.IMAGE_ROTATION_DEGREE);

        PowerManager.WakeLock wakeLock = getWakeLock(context);
        wakeLock.acquire();

        FileManager rotatedFile = rotateImage(baseFile, rotatedImageFilepath, degrees);

        releaseWakeLock(wakeLock);

        if (rotatedFile != null) {
            MediaFilesUtils.triggerMediaScanOnFile(context, rotatedFile.getFile());
            return rotatedFile;
        }

        return baseFile;
    }

    @Nullable
    private FileManager rotateImage(FileManager baseFile, String rotatedImageFilepath, int degrees) {
        FileManager rotatedFile = null;
        String imagePath = baseFile.getFileFullPath();
        Bitmap bmp = BitmapUtils.decodeSampledBitmapFromImageFile(imagePath);

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

        FileOutputStream fos = null;

        try {

            rotatedFile = new FileManager(rotatedImageFilepath);
            fos = new FileOutputStream(rotatedFile.getFile());
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos); // PNG is a lossless format, the compression factor (100) is ignored

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return rotatedFile;
    }

    @Nullable
    private FileManager compressVideo(FileManager baseFile, String compressedFilePath, Context context) {

        FileManager modifiedFile;

        int[] res = _ffmpeg_utils.getVideoResolution(baseFile);
        int width = res[0];
        int height = res[1];

        modifiedFile = _ffmpeg_utils.compressVideoFile(baseFile, compressedFilePath, width, height, context);
        return modifiedFile;
    }

    @Nullable
    private FileManager compressImage(FileManager baseFile, String compressedFilePath, Context context) {

        FileManager modifiedFile;

        if (baseFile.getFileExtension().equals("gif")) {

            int hz = 10;
            modifiedFile = _ffmpeg_utils.compressGifImageFile(baseFile, compressedFilePath, hz, context);

        } else {
            int width = _ffmpeg_utils.getImageResolution(baseFile)[0];

            modifiedFile = _ffmpeg_utils.compressImageFile(baseFile, compressedFilePath, width, context);
        }

        return modifiedFile;
    }

    private FileManager compressAudio(FileManager baseFile, String outputDir, Context context) {
//        if (baseFile.getFileSize() <= AUDIO_SIZE_COMPRESS_NEEDED)
        return baseFile; //TODO check if there is a way to compress non-wav audio files further
    }


    @Nullable
    private FileManager trimAudioAndVideo(FileManager baseFile, String trimmedFilePath, int sizeToCompress, Context context) {

        FileManager modifiedFile = null;
        long duration = _ffmpeg_utils.getFileDuration(context, baseFile);
        long startTime = SharedPrefUtils.getInt(context, SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_START_TRIM_IN_MILISEC);
        long endTime = SharedPrefUtils.getInt(context, SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_END_TRIM_IN_MILISEC) - SharedPrefUtils.getInt(context, SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_START_TRIM_IN_MILISEC);

        Log.i(TAG, "Start time: " + startTime + " Endtime: " + endTime);

        // Manual Trim from audio editor
        if (endTime > 0) {
            log(Log.INFO, TAG, "Performing manual trim");
            modifiedFile = _ffmpeg_utils.trim(baseFile, trimmedFilePath, startTime, endTime, context);
            duration = _ffmpeg_utils.getFileDuration(context, modifiedFile);
        }

        //Auto Trim
        if (duration > MAX_DURATION && baseFile.getFileSize() > sizeToCompress) {
            log(Log.INFO, TAG, "Performing auto trim");
            modifiedFile = _ffmpeg_utils.trim(baseFile, trimmedFilePath, MAX_DURATION, context);
        }

        return modifiedFile;
    }

    public boolean isCompressionNeeded(Context context, FileManager managedfile) {

        // Trim reduced file size enough - No need for compression
        if (isFileTrimmed(context, managedfile) && managedfile.getFileSize() < AFTER_TRIM_SIZE_COMPRESS_NEEDED)
            return false;

        if(isFileCompressed(context, managedfile))
            return false;

        switch (managedfile.getFileType()) {

            case VIDEO:
                if (managedfile.getFileSize() <= VIDEO_SIZE_COMPRESS_NEEDED)
                    return false;
                break;
            case AUDIO:
                if (managedfile.getFileSize() <= AUDIO_SIZE_COMPRESS_NEEDED)
                    return false;
                break;
            case IMAGE:
                if (managedfile.getFileSize() <= IMAGE_SIZE_COMPRESS_NEEDED)
                    return false;
        }

        return true;
    }

    public boolean isTrimNeeded(Context ctx, FileManager baseFile) {
        boolean isManualTrimNeeded = SharedPrefUtils.getInt(ctx, SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_END_TRIM_IN_MILISEC) > 0;
        boolean isAutoTrimNeeded = !baseFile.getFileType().equals(FileManager.FileType.IMAGE) &&
                (_ffmpeg_utils.getFileDuration(ctx, baseFile) > MediaFileProcessingUtils.MAX_DURATION)
                && (isCompressionNeeded(ctx, baseFile));

        return (isAutoTrimNeeded || isManualTrimNeeded);

    }

    public boolean isRotationNeeded(Context ctx, FileManager.FileType fileType) {
        return fileType.equals(FileManager.FileType.IMAGE) &&
                SharedPrefUtils.getInt(ctx, SharedPrefUtils.GENERAL, SharedPrefUtils.IMAGE_ROTATION_DEGREE) > 0;
    }


    private boolean isFileTrimmed(Context context, FileManager baseFile) {
        return SharedPrefUtils.getBoolean(context, SharedPrefUtils.TRIMMED_FILES, baseFile.getFile().getAbsolutePath());
    }

    private boolean isFileCompressed(Context context, FileManager baseFile) {
        return SharedPrefUtils.getBoolean(context, SharedPrefUtils.COMPRESSED_FILES, baseFile.getFile().getAbsolutePath());
    }

    private void markFilePathAsTrimmed(Context context, String trimmedFilePath) {
        SharedPrefUtils.setBoolean(context, SharedPrefUtils.TRIMMED_FILES, trimmedFilePath, true);
    }


    private void markFilePathAsCompressed(Context context, String compressedFilePath) {
        SharedPrefUtils.setBoolean(context, SharedPrefUtils.COMPRESSED_FILES, compressedFilePath, true);
    }


    private PowerManager.WakeLock getWakeLock(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Activity.POWER_SERVICE);
        return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VK_LOCK");
    }

    private void releaseWakeLock(PowerManager.WakeLock wakeLock) {
        if (wakeLock.isHeld()) {
            wakeLock.release();
            log(Log.INFO, TAG, "Wake lock released");
        } else {
            log(Log.INFO, TAG, "Wake lock is already released, doing nothing");
        }
    }

//    private void mapTrimmedFilePathToBaseFilePath(Context context, String baseFilePath, String trimmedFilePath) {
//    }

//    /**
//     * This method maps a filepath to its compressed filepath
//     * @param context Application context
//     * @param baseFilePath The filepath to map
//     * @param compressedFilePath The compressed filepath to map to
//     */
//    private void markFileAsCompressed(Context context, String baseFilePath, String compressedFilePath) {
//        SharedPrefUtils.setString(context, SharedPrefUtils.COMPRESSED_FILES, baseFilePath, compressedFilePath);
//        // Mapping the compressed file to itself so we won't compress it again
//        SharedPrefUtils.setString(context, SharedPrefUtils.COMPRESSED_FILES, compressedFilePath, compressedFilePath);
//    }

//    /**
//     * Returns the compressed file mapped to this file if exists, null otherwise.
//     * For already compressed files, the mapping should be the file itself.
//     * @param context Application context
//     * @param baseFile The file for which to get the compressed mapped to it
//     * @return
//     */
//    private FileManager getCompressedFile(Context context, FileManager baseFile) {
//        FileManager compressedFile = null;
//        try {
//
//            String compFilePath = SharedPrefUtils.getString(context, SharedPrefUtils.COMPRESSED_FILES, baseFile.get_uncompdFileFullPath());
//            if (!compFilePath.isEmpty()) {
//                compressedFile = new FileManager(compFilePath);
//            }
//        } catch(Exception ignored) {}
//
//        return compressedFile;
//    }

}
