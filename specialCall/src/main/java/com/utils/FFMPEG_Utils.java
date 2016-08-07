package com.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.data_objects.Constants;
import com.netcompss.loader.LoadJNI;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import FilesManager.FileManager;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by mor on 09/11/2015.
 */
public class FFMPEG_Utils {

    public static final double REDUCE_IMAGE_RES_MULTIPLIER = 0.7;
    public static final double REDUCE_VIDEO_RES_MULTIPLIER = 0.5;


    private static final String TAG = FFMPEG_Utils.class.getSimpleName();
    private static final String workFolder = Constants.COMPRESSED_FOLDER;
    private static final HashMap<String, String> extension2vCodec = new HashMap() {{
        put("mp4", "mpeg4");

    }};
    private LoadJNI _vk;
    private Handler _compressHandler;

    public FFMPEG_Utils(LoadJNI vk) {

        _vk = vk;
    }

    public FFMPEG_Utils(LoadJNI vk, Handler compressHandler) {

        _vk = vk;
        _compressHandler = compressHandler;
    }


    /**
     * Compresses a video by reducing bitrate.
     * If not enough, reduces resolution as well
     *
     * @param baseFile The base file to compress
     * @param compressedFilePath  The path of the compressed file
     * @param context Application context
     * @return The compressed file, if possible. Otherwise, null.
     */
    public FileManager compressVideoFile(FileManager baseFile, String compressedFilePath, double width, double height, Context context) {

        try {
            String vCodec = extension2vCodec.get(baseFile.getFileExtension());
            File compressedFile = new File(compressedFilePath);

            long duration = getFileDurationInSeconds(context, baseFile); // In seconds
            String bitrate = String.valueOf(MediaFileProcessingUtils.VIDEO_SIZE_COMPRESS_NEEDED * 8 / duration); // Units are bits/second

            // Command to reduce video bitrate
            String[] complexCommand =
                    {"ffmpeg", "-y", "-i", baseFile.getFile().getAbsolutePath(), "-strict", "experimental", "-s", (int) width + "x" + (int) height,
                            "-r", "25", "-vcodec", vCodec, "-b", bitrate, "-ab", "48000", "-ac", "2", "-ar", "22050",
                            compressedFilePath};

            _vk.run(complexCommand, workFolder, context);

            // If not enough we reduce resolution
            if (width > MediaFileProcessingUtils.MIN_RESOLUTION && compressedFile.length() > MediaFileProcessingUtils.VIDEO_SIZE_COMPRESS_NEEDED) {

                sendCompressionPhase2();

                double percent = REDUCE_VIDEO_RES_MULTIPLIER;
                width = width * percent;
                height = height * percent;

                complexCommand = new String[]
                        {"ffmpeg", "-y", "-i", baseFile.getCompFileFullPath(), "-strict", "experimental", "-s", (int) width + "x" + (int) height,
                                "-r", "25", "-vcodec", vCodec, "-b", bitrate, "-ab", "48000", "-ac", "2", "-ar", "22050",
                                compressedFilePath};

                _vk.run(complexCommand, workFolder, context);
            }

            return new FileManager(compressedFile);

        } catch (Throwable e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Compressing video file failed: " + e.getMessage());
        }

        // Could not compress
        return null;
    }

    /**
     * Resizes an image file resolution by 1-REDUCE_IMAGE_RES_MULTIPLIER, maintaining aspect ratio
     *
     * @param baseFile The base file to compress
     * @param compressedFilePath  The output of the compressed file
     * @param width    The width parameter of the original resolution
     * @param context Application context
     * @return The compressed file, if possible. Otherwise, null.
     */
    public FileManager compressImageFile(FileManager baseFile, String compressedFilePath, double width, Context context) {

        try {
            File compressedFile = new File(compressedFilePath);

            String[] complexCommand;

            double percent = REDUCE_IMAGE_RES_MULTIPLIER;
            width = width * percent;
            complexCommand = new String[]
                    {"ffmpeg", "-i", baseFile.getCompFileFullPath(), "-vf", "scale=" + (int) width + ":-1", compressedFilePath};

            _vk.run(complexCommand, workFolder, context);
            return new FileManager(compressedFile);

        } catch (Throwable e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Compressing image file failed: " + e.getMessage());
        }

        // Could not compress
        return null;
    }

    public FileManager compressGifImageFile(FileManager baseFile, String compressedFilePath, Integer hz, Context context) {

        try {
            File compressedFile = new File(compressedFilePath);

            String[] complexCommand = new String[]{"ffmpeg", "-f", "gif", "-i", baseFile.getFile().getAbsolutePath(), "-strict", "experimental", "-r", hz.toString(), compressedFilePath};
            _vk.run(complexCommand, workFolder, context);
            return new FileManager(compressedFile);

        } catch (Throwable e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Compressing image file failed: " + e.getMessage());
        }

        // Could not compress
        return null;
    }

    /**
     * Trims a video file from startTime seconds to endTime seconds, without re-encoding.
     *
     * @param baseFile  The video file to trim
     * @param trimmedFilepath   The path of the trimmed video/audio
     * @param startTime The time where to start the cut
     * @param endTime   The time to end the cut in
     * @param context
     * @return The trimmed video file, if possible. Otherwise, null.
     */
    public FileManager trimVideo(FileManager baseFile, String trimmedFilepath, Long startTime, Long endTime, Context context) {

        File trimmedFile = new File(trimmedFilepath);

        String start = convertMillisToTimeFormat(startTime);
        String end = convertMillisToTimeFormat(endTime);

        Log.i(TAG, "Trim Through Audio Editor, Start: " + start + " End: " + end);

        try {

            String[] complexCommand =
                    {"ffmpeg","-ss",start,"-y","-i",baseFile.getFile().getAbsolutePath(),"-strict",
                            "experimental","-acodec","aac","-ab","48000","-ac","2","-ar","22050","-t", end,"-vcodec",
                            "copy", trimmedFilepath};

            _vk.run(complexCommand, workFolder, context);
            return new FileManager(trimmedFile);

        } catch (Throwable e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Trimming file failed: " + e.getMessage());
        }
        // Could not trim
        return null;

    }

    /**
     * Trims an audio file from startTime seconds to endTime seconds, without re-encoding.
     *
     * @param baseFile  The audio file to trim
     * @param trimmedFilepath   The path of the trimmed audio
     * @param startTime The time where to start the cut
     * @param endTime   The time to end the cut in
     * @param context
     * @return The trimmed audio file, if possible. Otherwise, null.
     */
    public FileManager trimAudio(FileManager baseFile, String trimmedFilepath, Long startTime, Long endTime, Context context) {

        File trimmedFile = new File(trimmedFilepath);

        String start = convertMillisToTimeFormat(startTime);
        String end = convertMillisToTimeFormat(endTime);

        Log.i(TAG, "Trimming audio file. Start: " + start + " End: " + end);

        try {

            String[] complexCommand =
                    {"ffmpeg","-ss",start,"-y","-i", baseFile.getFile().getAbsolutePath(),"-strict"
                            ,"experimental","-acodec","copy","-t", end,
                            trimmedFilepath};

            _vk.run(complexCommand, workFolder, context);
            return new FileManager(trimmedFile);

        } catch (Throwable e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Trimming file failed: " + e.getMessage());
        }
        // Could not trim
        return null;

    }

    private String convertMillisToTimeFormat(Long time) {
        DateFormat formatter = new SimpleDateFormat("mm:ss");
        Date timeInMilli = new Date(time);
        return formatter.format(timeInMilli);
    }

    /**
     * Retrieves video resolution using MediaMetadataRetriever
     *
     * @param managedFile The video file to retrieve its resolution
     * @return The image resolution
     * @see MediaMetadataRetriever
     */
    public int[] getVideoResolution(FileManager managedFile) {

        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(managedFile.getFileFullPath());
        String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

        return new int[]{Integer.parseInt(width), Integer.parseInt(height)};
    }

    /**
     * Retrieves the file duration using MediaMetadataRetriever
     *
     * @param managedFile The file to retrieve its duration
     * @return The file duration in seconds
     * @see MediaMetadataRetriever
     */
    public long getFileDurationInSeconds(Context context, FileManager managedFile) {
        return getFileDurationInMilliSeconds(context, managedFile) / 1000;
    }

    /**
     * Retrieves the file duration using MediaMetadataRetriever
     *
     * @param managedFile The file to retrieve its duration
     * @return The file duration in milliseconds
     * @see MediaMetadataRetriever
     */
    public long getFileDurationInMilliSeconds(Context context, FileManager managedFile) {

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, Uri.fromFile(managedFile.getFile()));
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long timeInMilli = Long.parseLong(time);
        return timeInMilli;
    }

    /**
     * Retrieves image resolution by decoding to bitmap
     *
     * @param managedFile The image file to retrieve its resolution
     * @return The image resolution
     * @see BitmapFactory
     */
    public int[] getImageResolution(FileManager managedFile) {

        Bitmap bitmap = BitmapFactory.decodeFile(managedFile.getFileFullPath());
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        bitmap.recycle();

        return new int[]{width, height};
    }

    private void sendCompressionPhase2() {

        Message msg = new Message();
        msg.what = MediaFileProcessingUtils.COMPRESSION_PHASE_2;
        _compressHandler.sendMessage(msg);
    }
}
