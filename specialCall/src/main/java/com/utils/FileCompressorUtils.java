package com.utils;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import com.data_objects.Constants;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.loader.LoadJNI;

import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;

/**
 * Created by mor on 12/11/2015.
 */
public class FileCompressorUtils {

    private static final String TAG                         =   FileCompressorUtils.class.getSimpleName();

    //region Constants for handler messages
    public static final String COMPRESSION_ITER             =   "CompressionIteration";
    public static final int STOP_TRANSCODING_MSG            =   -1;
    public static final int FINISHED_TRANSCODING_MSG        =   0;
    public static final int COMPRESSION_PHASE_2             =   1;
    //endregion

    //region Constants for FFMPEG compression algorithms
    public static final String workFolder                   =   Constants.TEMP_COMPRESSED_FOLDER;
    public static final String VK_LOG_PATH                  =   workFolder + "vk.log";
    public static final int VIDEO_SIZE_COMPRESS_NEEDED      =   3145728; // 3MB
    public static final int AUDIO_SIZE_COMPRESS_NEEDED      =   3145728; // 3MB
    public static final int IMAGE_SIZE_COMPRESS_NEEDED      =   1048576; // 1MB
    public static final long MAX_DURATION                   =   30;      // seconds
    public static final int MIN_RESOLUTION                  =   320;     // MIN width resolution
    private static final int MIN_HZ_FOR_GIF                 =   3;       // Num of frames in GIF
    //endregion

    private FFMPEG_Utils _ffmpeg_utils;
    private PowerManager.WakeLock _wakeLock;
    private Handler _compressHandler;

    public FileCompressorUtils(LoadJNI vk, PowerManager.WakeLock wakeLock, Handler compressHandler) {

        _compressHandler = compressHandler;
        _ffmpeg_utils = new FFMPEG_Utils(vk, compressHandler);
        _wakeLock = wakeLock;
    }

    public FileCompressorUtils(LoadJNI vk, PowerManager.WakeLock wakeLock) {

        _ffmpeg_utils = new FFMPEG_Utils(vk);
        _wakeLock = wakeLock;
    }

    public FileManager trimFileIfNecessary(FileManager baseFile, String folderName, Context context) {

        String outPath = workFolder + folderName;
        FileManager modifiedFile = baseFile;
        FileManager.FileType type = baseFile.getFileType();

        Log.d(TAG, "Acquire wake lock");
        _wakeLock.acquire();

        if(type.equals(FileManager.FileType.AUDIO))
            modifiedFile = trimMediaFile(baseFile, outPath, AUDIO_SIZE_COMPRESS_NEEDED ,context);

        if(type.equals(FileManager.FileType.VIDEO))
            modifiedFile = trimMediaFile(baseFile, outPath, VIDEO_SIZE_COMPRESS_NEEDED ,context);

        if (_wakeLock.isHeld()) {
            _wakeLock.release();
            Log.i(TAG, "Wake lock released");
        }
        else {
            Log.i(TAG, "Wake lock is already released, doing nothing");
        }

        return modifiedFile;
    }

    public static boolean isCompressionNeeded(FileManager managedfile) {

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

    /**
     * Compresses all file formats
     *
     * @param baseFile - The file to reduce its size if necessary
     * @param folderName  - The folder name to save the compressed file in
     * @param context
     * @return The compressed file, if necessary (and possible). Otherwise, the base file.
     */
    public FileManager compressFileIfNecessary(FileManager baseFile, String folderName, Context context) {

        if (GeneralUtils.isLicenseValid(context, workFolder) < 0)
            return baseFile;

        String outPath = Constants.TEMP_COMPRESSED_FOLDER + folderName;

        Log.d(TAG, "Acquire wake lock");
        _wakeLock.acquire();

            switch (baseFile.getFileType()) {

                case VIDEO:
                    return compressVideo(baseFile, outPath, context);
                case AUDIO:
                    return compressAudio(baseFile, outPath, context);
                case IMAGE:
                    return compressImage(baseFile, outPath, context);
            }

            if (_wakeLock.isHeld()) {
                _wakeLock.release();
                Log.i(TAG, "Wake lock released");
            }
            else {
                Log.i(TAG, "Wake lock is already released, doing nothing");
            }

        return baseFile;
    }

    private FileManager compressVideo(FileManager baseFile, String outPath, Context context) {

        FileManager modifiedFile;
        if (baseFile.getFileSize() <= VIDEO_SIZE_COMPRESS_NEEDED)
            return baseFile;

        BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.COMPRESSING, null, null));

        int[] res = _ffmpeg_utils.getVideoResolution(baseFile);
        int width = res[0];
        int height = res[1];

        modifiedFile = _ffmpeg_utils.compressVideoFile(baseFile, outPath, width, height, context);
        modifiedFile.set_uncompdFileFullPath(baseFile.getFileFullPath());
        modifiedFile.setIsCompressed(true);
        return modifiedFile;
    }

    private FileManager compressImage(FileManager baseFile, String outPath, Context context) {

        FileManager modifiedFile = baseFile;
        if (baseFile.getFileSize() <= IMAGE_SIZE_COMPRESS_NEEDED)
            return baseFile;

        BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.COMPRESSING, null, null));

        if(modifiedFile.getFileExtension().equals("gif")) {

            int hz = 10;
            modifiedFile = _ffmpeg_utils.compressGifImageFile(modifiedFile, outPath, hz, context);

            for (int cnt = 1; hz > MIN_HZ_FOR_GIF && modifiedFile.getFileSize() > IMAGE_SIZE_COMPRESS_NEEDED; cnt++) {

                hz/=2;
                sendIterationToHandler(cnt);
                modifiedFile = _ffmpeg_utils.compressGifImageFile(modifiedFile, outPath, hz, context);
            }
        }
        else {
            int width = _ffmpeg_utils.getImageResolution(baseFile)[0];

            modifiedFile = _ffmpeg_utils.compressImageFile(modifiedFile, outPath, width, context);
            width = _ffmpeg_utils.getImageResolution(modifiedFile)[0];

            // If image resolution is larger than MIN_RESOLUTION we start lowering resolution
            for (int cnt = 1; width > MIN_RESOLUTION && modifiedFile.getFileSize() > IMAGE_SIZE_COMPRESS_NEEDED ; cnt++) {

                sendIterationToHandler(cnt);
                modifiedFile = _ffmpeg_utils.compressImageFile(modifiedFile, outPath, width, context);
                width = _ffmpeg_utils.getImageResolution(modifiedFile)[0];
            }
        }

        modifiedFile.set_uncompdFileFullPath(baseFile.getFileFullPath());
        modifiedFile.setIsCompressed(true);
        return modifiedFile;
    }

    private FileManager compressAudio(FileManager baseFile, String outPath, Context context) {

        FileManager modifiedFile;
        if (baseFile.getFileSize() <= AUDIO_SIZE_COMPRESS_NEEDED)
            return baseFile;

        BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.COMPRESSING, null, null));

        //if (modifiedFile.getFileSize() >= AUDIO_SIZE_COMPRESS_NEEDED)
        // TODO See if there is a way to compress non-wav audio files

        modifiedFile = baseFile;

        modifiedFile.set_uncompdFileFullPath(baseFile.getFileFullPath());
        modifiedFile.setIsCompressed(true);
        return modifiedFile;

    }

    private FileManager trimMediaFile(FileManager baseFile, String outPath, int sizeToCompress, Context context) {

        FileManager modifiedFile = baseFile;
        long duration = _ffmpeg_utils.getFileDuration(context, modifiedFile);

        if (duration > MAX_DURATION && modifiedFile.getFileSize() > sizeToCompress) {
            modifiedFile = _ffmpeg_utils.trim(modifiedFile, outPath, MAX_DURATION, context);
        }

        modifiedFile.set_uncompdFileFullPath(baseFile.getFileFullPath());
        modifiedFile.setIsCompressed(true);
        return modifiedFile;
    }

    private void sendIterationToHandler(int iterationNum) {

        Bundle bundle = new Bundle(1);
        bundle.putInt(FileCompressorUtils.COMPRESSION_ITER, iterationNum);
        Message msg = new Message();
        msg.setData(bundle);
        msg.what = FileCompressorUtils.COMPRESSION_PHASE_2;
        _compressHandler.sendMessage(msg);
    }
}
