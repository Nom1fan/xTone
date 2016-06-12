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
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.loader.LoadJNI;

import java.io.File;
import java.util.HashMap;

import FilesManager.FileManager;

/**
 * Created by mor on 09/11/2015.
 */
public class FFMPEG_Utils {

    public static final double REDUCE_IMAGE_RES_MULTIPLIER = 0.7;
    public static final double REDUCE_VIDEO_RES_MULTIPLIER = 0.5;


    private static final String TAG = FFMPEG_Utils.class.getSimpleName();
    private static final String workFolder = Constants.TEMP_COMPRESSED_FOLDER;
    private static final HashMap<String, String> extension2vCodec = new HashMap() {{
        put("mp4", "mpeg4");

    }};
    private LoadJNI _vk;
    private Handler _compressHandler;

    public FFMPEG_Utils() {

    }

    public FFMPEG_Utils(LoadJNI vk) {

        _vk = vk;
    }

    public FFMPEG_Utils(LoadJNI vk, Handler compressHandler) {

        _vk = vk;
        _compressHandler = compressHandler;
    }


    /**
     * Resizes a video file resolution by 30%, maintaining aspect ratio.
     *
     * @param baseFile The base file to compress
     * @param outPath  The path of the compressed file
     * @param context
     * @return The compressed file.
     */
    public FileManager compressVideoFile(FileManager baseFile, String outPath, double width, double height, Context context) {

        String extension = baseFile.getFileExtension();
        String vCodec = extension2vCodec.get(extension);
        File compressedFile = new File(outPath + "/" + baseFile.getNameWithoutExtension() + "_comp." + extension);
        if (compressedFile.exists())
            FileManager.delete(compressedFile);

        try {
            long duration = getFileDuration(context, baseFile); // In seconds
            String bitrate = String.valueOf(FileCompressorUtils.VIDEO_SIZE_COMPRESS_NEEDED * 8 / duration); // Units are bits/second

            // Command to reduce video bitrate
            String[] complexCommand =
                    {"ffmpeg", "-y", "-i", baseFile.getCompFileFullPath(), "-strict", "experimental", "-s", (int) width + "x" + (int) height,
                            "-r", "25", "-vcodec", vCodec, "-b", bitrate, "-ab", "48000", "-ac", "2", "-ar", "22050",
                            compressedFile.getAbsolutePath()};

            _vk.run(complexCommand, workFolder, context);

            // If not enough we reduce resolution
            if (width > FileCompressorUtils.MIN_RESOLUTION && compressedFile.length() > FileCompressorUtils.VIDEO_SIZE_COMPRESS_NEEDED) {

                sendCompressionPhase2();

                double percent = REDUCE_VIDEO_RES_MULTIPLIER;
                width = width * percent;
                height = height * percent;

                complexCommand = new String[]
                        {"ffmpeg", "-y", "-i", baseFile.getCompFileFullPath(), "-strict", "experimental", "-s", (int) width + "x" + (int) height,
                                "-r", "25", "-vcodec", vCodec, "-b", bitrate, "-ab", "48000", "-ac", "2", "-ar", "22050",
                                compressedFile.getAbsolutePath()};

                _vk.run(complexCommand, workFolder, context);
            }

            return new FileManager(compressedFile);

        } catch (Throwable e) {
            Log.e(TAG, "Compressing video file failed", e);
        }

        // Could not compress, returning uncompressed (untouched) file
        return baseFile;
    }

    /**
     * Resizes an image file resolution by 30%, maintaining aspect ratio
     *
     * @param baseFile The base file to compress
     * @param outPath  The output of the compressed file
     * @param width    The width parameter of the original resolution
     * @param context
     * @return The compressed file.
     */
    public FileManager compressImageFile(FileManager baseFile, String outPath, double width, Context context) {

        String extension = baseFile.getFileExtension();
        File compressedFile = new File(outPath + "/" + baseFile.getNameWithoutExtension() + "_comp." + extension);
        if (compressedFile.exists())
            FileManager.delete(compressedFile);

        try {

            String[] complexCommand;

            double percent = REDUCE_IMAGE_RES_MULTIPLIER;
            width = width * percent;
            complexCommand = new String[]
                    {"ffmpeg", "-i", baseFile.getCompFileFullPath(), "-vf", "scale=" + (int) width + ":-1", compressedFile.getAbsolutePath()};

            _vk.run(complexCommand, workFolder, context);
            return new FileManager(compressedFile);

        } catch (Throwable e) {
            Log.e(TAG, "Compressing image file failed", e);
        }

        // Could not compress, returning uncompressed (untouched) file
        return baseFile;
    }

    public FileManager compressGifImageFile(FileManager baseFile, String outPath, Integer hz, Context context) {

        String extension = baseFile.getFileExtension();
        File compressedFile = new File(outPath + "/" + baseFile.getNameWithoutExtension() + "_comp." + extension);
        if (compressedFile.exists())
            FileManager.delete(compressedFile);

        try {

            String[] complexCommand = new String[]{"ffmpeg", "-f", "gif", "-i", baseFile.getCompFileFullPath(), "-strict", "experimental", "-r", hz.toString(), compressedFile.getAbsolutePath()};
            _vk.run(complexCommand, workFolder, context);
            return new FileManager(compressedFile);

        } catch (Throwable e) {
            Log.e(TAG, "Compressing image file failed", e);
        }

        return baseFile;
    }

    /**
     * Trims a video/audio file from 0 seconds to endTime seconds, without re-encoding.
     *
     * @param baseFile Video/audio file to trim
     * @param outPath  The path of the trimmed video/audio
     * @param endTime  The time to end the cut in
     * @param context
     * @return The trimmed video/audio file, if possible. Otherwise, the base file.
     */
    public FileManager trim(FileManager baseFile, String outPath, Long endTime, Context context) {

        if (GeneralUtils.isLicenseValid(context, workFolder) < 0)
            return baseFile;

        String extension = baseFile.getFileExtension();
        File trimmedFile = new File(outPath + "/" + baseFile.getNameWithoutExtension() + "_trimmed." + extension);
        if (trimmedFile.exists())
            FileManager.delete(trimmedFile);

        try {


            String[] complexCommand =
                    {"ffmpeg", "-i", baseFile.getFileFullPath(), "-vcodec", "copy", "-acodec",
                            "copy", "-ss", "0", "-t", endTime.toString(), trimmedFile.getAbsolutePath()};

            _vk.run(complexCommand, workFolder, context);
            return new FileManager(trimmedFile);

        } catch (Throwable e) {
            Log.e(TAG, "Trimming file failed", e);
        }
        // Could not trim, returning untrimmed (untouched) file
        return baseFile;

    }

    /**
     * Retrieves video resolution using MediaMetadataRetriever
     * @see MediaMetadataRetriever
     * @param managedFile The video file to retrieve its resolution
     * @return The image resolution
     */
    public int[] getVideoResolution(FileManager managedFile) {

        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(managedFile.getFileFullPath());
        String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

        return new int[] { Integer.parseInt(width), Integer.parseInt(height) };
    }

    /**
     * Retrieves the file duration using MediaMetadataRetriever
     * @see MediaMetadataRetriever
     * @param managedFile The file to retrieve its duration
     * @return The file duration in seconds
     */
    public long getFileDuration(Context context, FileManager managedFile) {

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, Uri.fromFile(managedFile.getFile()));
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long timeInMilli = Long.parseLong(time );
        return timeInMilli/1000;
    }

    /**
     * Retrieves image resolution by decoding to bitmap
     * @see BitmapFactory
     * @param managedFile The image file to retrieve its resolution
     * @return The image resolution
     */
    public int[] getImageResolution(FileManager managedFile) {

        Bitmap bitmap = BitmapFactory.decodeFile(managedFile.getFileFullPath());
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        bitmap.recycle();

        return new int[] {  width, height };
    }

    private void sendCompressionPhase2() {

        Message msg = new Message();
        msg.what = FileCompressorUtils.COMPRESSION_PHASE_2;
        _compressHandler.sendMessage(msg);
    }
}
