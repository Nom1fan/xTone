package com.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.data.objects.Constants;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.loader.LoadJNI;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.files.media.MediaFile;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by mor on 12/11/2015.
 */
public class MediaFileProcessingUtils {


    //region Constants for FFMPEG compression algorithms
    public static final String workFolder = Constants.COMPRESSED_FOLDER;
    public static final String VK_LOG_PATH = workFolder + "vk.log";
    public static final int VIDEO_SIZE_COMPRESS_NEEDED = 4194304; // 4MB
    public static final int AUDIO_SIZE_COMPRESS_NEEDED = 4194304; // 4MB
    public static final int IMAGE_SIZE_COMPRESS_NEEDED = 2097152; // 1MB
    public static final int AFTER_TRIM_SIZE_COMPRESS_NEEDED = 4194304; // 4MB
    public static final long MAX_DURATION = 61 * 1000;      // Milliseconds
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


    //region Main media file processing methods
    public MediaFile trimMediaFile(MediaFile baseFile, String trimmedFilePath, Context context) {

        if (GeneralUtils.isLicenseValid(context, workFolder) < 0) {
            return baseFile;
        }

        MediaFile trimmedFile = null;
        MediaFile.FileType type = baseFile.getFileType();

        Log.d(TAG, "Acquire wake lock");
        PowerManager.WakeLock wakeLock = getWakeLock(context);
        wakeLock.acquire();

        if (type.equals(MediaFile.FileType.AUDIO))
            trimmedFile = trimAudio(baseFile, trimmedFilePath, context);

        if (type.equals(MediaFile.FileType.VIDEO))
            trimmedFile = trimVideo(baseFile, trimmedFilePath, context);

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
     * @param context  Application context
     * @return The compressed file, if necessary (and possible). Otherwise, the base file.
     */
    public MediaFile compressMediaFile(MediaFile baseFile, String compressedFilePath, Context context) {

        if (GeneralUtils.isLicenseValid(context, workFolder) < 0) {
            return baseFile;
        }

        MediaFile alreadyCompFile = getAlreadyCompFile(baseFile);
        if (alreadyCompFile != null)
            return alreadyCompFile;

        MediaFile compressedFile = null;

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

        if (compressedFile != null) {
            // This means we compressed a trimmed file which was still too large - Deleting trimmed to avoid duplicates
            if (isFileTrimmed(context, baseFile)) {
                String trimmedFilePath = baseFile.getFile().getAbsolutePath();
                SharedPrefUtils.remove(context, SharedPrefUtils.TRIMMED_FILES, trimmedFilePath);
                MediaFilesUtils.delete(baseFile.getFile());
            }

            MediaFilesUtils.triggerMediaScanOnFile(context, compressedFile.getFile());
            return compressedFile;
        }

        return baseFile;
    }

    public MediaFile rotateImageFile(MediaFile baseFile, String rotatedImageFilepath, Context context) {

        int degrees = SharedPrefUtils.getInt(context, SharedPrefUtils.GENERAL, SharedPrefUtils.IMAGE_ROTATION_DEGREE);

        PowerManager.WakeLock wakeLock = getWakeLock(context);
        wakeLock.acquire();

        MediaFile rotatedFile = rotateImage(baseFile, rotatedImageFilepath, degrees);

        releaseWakeLock(wakeLock);

        if (rotatedFile != null) {
            MediaFilesUtils.triggerMediaScanOnFile(context, rotatedFile.getFile());
            return rotatedFile;
        }

        return baseFile;
    }
    //endregion

    //region Sub media file proecessing methods
    @Nullable
    private MediaFile rotateImage(MediaFile baseFile, String rotatedImageFilepath, int degrees) {
        File rotatedFile = null;
        String imagePath = baseFile.getFileFullPath();
        Bitmap bmp = BitmapUtils.decodeSampledBitmapFromImageFile(imagePath);

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

        FileOutputStream fos = null;

        try {

            rotatedFile = new File(rotatedImageFilepath);
            fos = new FileOutputStream(rotatedFile);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos); // PNG is a lossless format, the compression factor (100) is ignored
            return new MediaFile(rotatedFile);

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

        // Could not rotate
        return null;
    }

    @Nullable
    private MediaFile compressVideo(MediaFile baseFile, String compressedFilePath, Context context) {

        MediaFile modifiedFile;

        int[] res = _ffmpeg_utils.getVideoResolution(baseFile);
        int width = res[0];
        int height = res[1];

        modifiedFile = _ffmpeg_utils.compressVideoFile(baseFile, compressedFilePath, width, height, context);
        return modifiedFile;
    }

    @Nullable
    private MediaFile compressImage(MediaFile baseFile, String compressedFilePath, Context context) {

        MediaFile modifiedFile;

        if (baseFile.getFileExtension().equals("gif")) {

            int hz = 10;
            modifiedFile = _ffmpeg_utils.compressGifImageFile(baseFile, compressedFilePath, hz, context);

        } else {
            int width = _ffmpeg_utils.getImageResolution(baseFile)[0];

            modifiedFile = _ffmpeg_utils.compressImageFile(baseFile, compressedFilePath, width, context);
        }

        return modifiedFile;
    }

    private MediaFile compressAudio(MediaFile baseFile, String outputDir, Context context) {
        return baseFile; //TODO check if there is a way to compress non-wav audio files further
    }

    @Nullable
    private MediaFile trimVideo(MediaFile baseFile, String trimmedFilePath, Context context) {

        MediaFile modifiedFile = null;
        TrimData trimData = new TrimData().getTrimData(context, "video");
        long startTime = trimData.getStartTime();
        long endTime = trimData.getEndTime();

        modifiedFile = _ffmpeg_utils.trimVideo(baseFile, trimmedFilePath, startTime, endTime, context);
        return modifiedFile;
    }

    @Nullable
    private MediaFile trimAudio(MediaFile baseFile, String trimmedFilePath, Context context) {

        MediaFile modifiedFile = null;
        TrimData trimData = new TrimData().getTrimData(context, "audio");
        long startTime = trimData.getStartTime();
        long endTime = trimData.getEndTime();

        modifiedFile = _ffmpeg_utils.trimAudio(baseFile, trimmedFilePath, startTime, endTime, context);
        return modifiedFile;
    }
    //endregion

    //region Helper methods
    public boolean isCompressionNeeded(Context context, MediaFile managedfile) {

        // Trim reduced file size enough - No need for compression
        if (isFileTrimmed(context, managedfile) && managedfile.getFileSize() < AFTER_TRIM_SIZE_COMPRESS_NEEDED)
            return false;

        if (isFileCompressed(context, managedfile))
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

    public boolean isTrimNeeded(Context ctx, MediaFile baseFile) {
        boolean isManualTrimNeeded = SharedPrefUtils.getInt(ctx, SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_VIDEO_END_TRIM_IN_MILISEC) > 0;
        boolean isAutoTrimNeeded = !baseFile.getFileType().equals(MediaFile.FileType.IMAGE) &&
                (MediaFilesUtils.getFileDurationInMilliSeconds(ctx, baseFile) > MediaFileProcessingUtils.MAX_DURATION)
                && (isCompressionNeeded(ctx, baseFile));

        return (isAutoTrimNeeded || isManualTrimNeeded);

    }

    public boolean isRotationNeeded(Context ctx, MediaFile.FileType fileType) {
        return fileType.equals(MediaFile.FileType.IMAGE) &&
                SharedPrefUtils.getInt(ctx, SharedPrefUtils.GENERAL, SharedPrefUtils.IMAGE_ROTATION_DEGREE) > 0;
    }

    private boolean isFileTrimmed(Context context, MediaFile baseFile) {
        return SharedPrefUtils.getBoolean(context, SharedPrefUtils.TRIMMED_FILES, baseFile.getFile().getAbsolutePath());
    }

    private boolean isFileCompressed(Context context, MediaFile baseFile) {
        boolean isComp = false;
        try {
            isComp = FileUtils.directoryContains(new File(Constants.COMPRESSED_FOLDER), baseFile.getFile()) && !isFileTrimmed(context, baseFile);
        } catch (IOException e) {
            log(Log.WARN, TAG, "Failed to determine if file was previously compressed. Exception:" + e.getMessage());
        }
        return isComp;
    }

    private void markFilePathAsTrimmed(Context context, String trimmedFilePath) {
        SharedPrefUtils.setBoolean(context, SharedPrefUtils.TRIMMED_FILES, trimmedFilePath, true);
    }

    @Nullable
    private MediaFile getAlreadyCompFile(MediaFile baseFile) {
        MediaFile compressedFile = null;

        try {
            File potentialCompFile = MediaFilesUtils.getFileByMD5(baseFile.getMd5(), Constants.COMPRESSED_FOLDER);

            // File already has a previously compressed file in compressed folder
            if (potentialCompFile != null) {
                compressedFile = MediaFilesUtils.createMediaFile(potentialCompFile);
            }
        } catch (Exception e) {
            log(Log.WARN, TAG, "Failed to retrieve previously compressed file. Exception:" + e.getMessage());
        }
        return compressedFile;
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
    //endregion

    //region Data objects
    private class TrimData {
        private long startTime;
        private long endTime;

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public TrimData getTrimData(Context context, String action) {
            startTime = SharedPrefUtils.getInt(context, SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_VIDEO_START_TRIM_IN_MILISEC);
            endTime = SharedPrefUtils.getInt(context, SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_VIDEO_END_TRIM_IN_MILISEC) - SharedPrefUtils.getInt(context, SharedPrefUtils.GENERAL, SharedPrefUtils.AUDIO_VIDEO_START_TRIM_IN_MILISEC);
            boolean autoTrim = endTime <= 0;

            Log.i(TAG, "Start time:" + startTime + " End time:" + endTime);

            if (autoTrim) {
                log(Log.INFO, TAG, "Performing auto trim " + action);
                endTime = MAX_DURATION;
            } else {
                log(Log.INFO, TAG, "Performing manual trim " + action);
                if (endTime > MAX_DURATION)
                    endTime = MAX_DURATION;
            }
            return this;
        }
    }
    //endregion

}