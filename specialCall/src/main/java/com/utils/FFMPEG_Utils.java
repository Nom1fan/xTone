package com.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.data.objects.Constants;
import com.netcompss.loader.LoadJNI;

import org.apache.commons.lang.time.DurationFormatUtils;

import java.io.File;
import java.util.HashMap;

import com.files.media.MediaFile;

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
    private BitmapUtils bitmapUtils = UtilityFactory.instance().getUtility(BitmapUtils.class);
    private MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);

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
    public MediaFile compressVideoFile(MediaFile baseFile, String compressedFilePath, double width, double height, Context context) {

        try {
            String vCodec = extension2vCodec.get(baseFile.getExtension());
            File compressedFile = new File(compressedFilePath);

            long duration = mediaFileUtils.getFileDurationInSeconds(context, baseFile); // In seconds
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
                        {"ffmpeg", "-y", "-i", baseFile.getFile().getAbsolutePath(), "-strict", "experimental", "-s", (int) width + "x" + (int) height,
                                "-r", "25", "-vcodec", vCodec, "-b", bitrate, "-ab", "48000", "-ac", "2", "-ar", "22050",
                                compressedFilePath};

                _vk.run(complexCommand, workFolder, context);
            }

            return new MediaFile(compressedFile);

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
    public MediaFile compressImageFile(MediaFile baseFile, String compressedFilePath, double width, Context context) {

        try {
            File compressedFile = new File(compressedFilePath);

            String[] complexCommand;

            double percent = REDUCE_IMAGE_RES_MULTIPLIER;
            width = width * percent;
            complexCommand = new String[]
                    {"ffmpeg", "-i", baseFile.getFile().getAbsolutePath(), "-vf", "scale=" + (int) width + ":-1", compressedFilePath};

            _vk.run(complexCommand, workFolder, context);
            return new MediaFile(compressedFile);

        } catch (Throwable e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Compressing image file failed: " + e.getMessage());
        }

        // Could not compress
        return null;
    }

    public MediaFile compressGifImageFile(MediaFile baseFile, String compressedFilePath, Integer hz, Context context) {

        try {
            File compressedFile = new File(compressedFilePath);

            String[] complexCommand = new String[]{"ffmpeg", "-f", "gif", "-i", baseFile.getFile().getAbsolutePath(), "-strict", "experimental", "-r", hz.toString(), compressedFilePath};
            _vk.run(complexCommand, workFolder, context);
            return new MediaFile(compressedFile);

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
    public MediaFile trimVideo(MediaFile baseFile, String trimmedFilepath, Long startTime, Long endTime, Context context) {

        File trimmedFile = new File(trimmedFilepath);

        String start = convertMillisToTimeFormat(startTime);
        String end = convertMillisToTimeFormat(endTime);

        SharedPrefUtils.setInt(context, SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_START_TRIM_IN_MILISEC, 0);
        SharedPrefUtils.setInt(context, SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_END_TRIM_IN_MILISEC, 0);

        Log.i(TAG, "Trimming video. Start: " + start + " End: " + end);

        try {

            String[] complexCommand =
                    {"ffmpeg","-ss", start,"-y","-itsoffset","0.1","-i",baseFile.getFile().getAbsolutePath(),"-strict",
                            "experimental","-acodec","copy","-t",end,"-vcodec",
                            "copy",trimmedFilepath};

//            String[] complexCommand =
//                    {"ffmpeg","-ss",start,"-y","-i",baseFile.getFile().getAbsolutePath(),"-strict",
//                            "experimental","-acodec","aac","-ab","48000","-ac","2","-ar","22050","-t", end,"-vcodec",
//                            "copy", trimmedFilepath};

            _vk.run(complexCommand, workFolder, context);
            return new MediaFile(trimmedFile);

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
    public MediaFile trimAudio(MediaFile baseFile, String trimmedFilepath, Long startTime, Long endTime, Context context) {

        File trimmedFile = new File(trimmedFilepath);

        String start = convertMillisToTimeFormat(startTime);
        String end = convertMillisToTimeFormat(endTime);

        SharedPrefUtils.setInt(context, SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_START_TRIM_IN_MILISEC, 0);
        SharedPrefUtils.setInt(context, SharedPrefUtils.GENERAL,SharedPrefUtils.AUDIO_VIDEO_END_TRIM_IN_MILISEC, 0);

        Log.i(TAG, "Trimming audio. Start: " + start + " End: " + end);

        try {

            String[] complexCommand =
                    {"ffmpeg","-ss",start,"-y","-i", baseFile.getFile().getAbsolutePath(),"-strict"
                            ,"experimental","-acodec","copy","-t", end,
                            trimmedFilepath};

            _vk.run(complexCommand, workFolder, context);
            return new MediaFile(trimmedFile);

        } catch (Throwable e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Trimming file failed: " + e.getMessage());
        }
        // Could not trim
        return null;

    }

    private String convertMillisToTimeFormat(Long time) {
       return DurationFormatUtils.formatDurationHMS(time);
    }

    /**
     * Retrieves video resolution using MediaMetadataRetriever
     *
     * @param managedFile The video file to retrieve its resolution
     * @return The image resolution
     * @see MediaMetadataRetriever
     */
    public int[] getVideoResolution(MediaFile managedFile) {

        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(managedFile.getFile().getAbsolutePath());
        String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

        return new int[]{Integer.parseInt(width), Integer.parseInt(height)};
    }

    /**
     * Retrieves image resolution by decoding to bitmap
     *
     * @param managedFile The image file to retrieve its resolution
     * @return The image resolution
     * @see BitmapFactory
     */
    public int[] getImageResolution(MediaFile managedFile) {

        Bitmap bitmap = bitmapUtils.decodeSampledBitmapFromImageFile(managedFile.getFile().getAbsolutePath());
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
