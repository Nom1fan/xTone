package com.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.data_objects.Constants;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.loader.LoadJNI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import FilesManager.FileManager;

/**
 * Created by mor on 09/11/2015.
 */
public class FFMPEG_Utils {

    private static final String TAG = FFMPEG_Utils.class.getSimpleName();
    private static final String workFolder = Constants.TEMP_COMPRESSED_FOLDER;
    private static final HashMap<String, String> extension2vCodec = new HashMap() {{
        put("mp4", "mpeg4");

    }};
    private LoadJNI _vk;

    public FFMPEG_Utils(LoadJNI vk) {

        _vk = vk;
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
//            double percent = 0.7;
//            width = width * percent;
//            height = height * percent;

            String[] complexCommand =
                    {"ffmpeg", "-y", "-i", baseFile.getFileFullPath(), "-strict", "experimental", "-s", (int) width + "x" + (int) height,
                            "-r", "25", "-vcodec", vCodec, "-b", "150k", "-ab", "48000", "-ac", "2", "-ar", "22050",
                            compressedFile.getAbsolutePath()};

            _vk.run(complexCommand, workFolder, context);
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
            double percent = 0.7;
            width = width * percent;

            String[] complexCommand =
                    {"ffmpeg", "-i", baseFile.getFileFullPath(), "-vf", "scale=" + (int) width + ":-1", compressedFile.getAbsolutePath()};

            _vk.run(complexCommand, workFolder, context);
            return new FileManager(compressedFile);

        } catch (Throwable e) {
            Log.e(TAG, "Compressing image file failed", e);
        }

        // Could not compress, returning uncompressed (untouched) file
        return baseFile;
    }

    /**
     * getFileDurationInSeconds
     *
     * @param baseFile The Video/audio file to retrieve its duration
     * @param context
     * @return The duration of a video/audio file in seconds, if possible. Otherwise returns 0.
     */
    public long getFileDurationInSeconds(FileManager baseFile, Context context) {

        if (GeneralUtils.isLicenseValid(context, workFolder) < 0)
            return 0;

        try {

            FileManager.FileType fType = baseFile.getFileType();
            if (fType.equals(FileManager.FileType.AUDIO) ||
                    fType.equals(FileManager.FileType.VIDEO)) {

                String[] complexCommand =
                        {"ffmpeg", "-i", baseFile.getFileFullPath()};
                _vk.run(complexCommand, workFolder, context);
                BufferedReader br = new BufferedReader(new FileReader(workFolder + "vk.log"));
                String line = "";
                boolean cont = true;

                while (cont && (line = br.readLine()) != null) {
                    if (line.contains("Duration"))
                        cont = false;
                }
                String unformattedDuration = line != null ? line.substring(12, 20) : null;
                DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                Date reference = dateFormat.parse("00:00:00");
                Date date = dateFormat.parse(unformattedDuration);
                long seconds = (date.getTime() - reference.getTime()) / 1000L;
                return seconds;
            }

        } catch (Throwable e) {
            Log.e(TAG, "Getting duration of file failed", e);
        }

        return 0;

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
    public FileManager trim(FileManager baseFile, String outPath, Double endTime, Context context) {

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
     * Retrieves image resolution from vk.log
     *
     * @param managedFile The image file to retrieve
     * @param context
     * @return The image resolution as integer array (width in 0 index and height in 1), if possible. Otherwise, returns null.
     * @deprecated Use the new {@link #getImageResolution(FileManager managedFile)} instead
     */
    @Deprecated
    public int[] getImageResolution(FileManager managedFile, Context context) {

        if (GeneralUtils.isLicenseValid(context, workFolder) < 0)
            return null;

        try {

            FileManager.FileType fType = managedFile.getFileType();
            if (fType.equals(FileManager.FileType.IMAGE)) {

                String[] complexCommand =
                        {"ffmpeg", "-i", managedFile.getFileFullPath()};
                _vk.run(complexCommand, workFolder, context);
                BufferedReader br = new BufferedReader(new FileReader(workFolder + "vk.log"));
                String line = "";
                boolean cont = true;

                while (cont && (line = br.readLine()) != null) {
                    if (line.contains("sof0"))
                        cont = false;
                }

                int pivot_index = 0;
                if (line != null) {
                    pivot_index = line.lastIndexOf(" ") + 1;
                }
                String unformattedResolution = null;
                if (line != null) {
                    unformattedResolution = line.substring(pivot_index, line.length());
                }
                String[] sArrayTmp = new String[0];
                if (unformattedResolution != null) {
                    sArrayTmp = unformattedResolution.split("x");
                }
                int[] iArraytmp = new int[2];
                iArraytmp[0] = Integer.parseInt(sArrayTmp[0]);
                iArraytmp[1] = Integer.parseInt(sArrayTmp[1]);

                return iArraytmp;
            }
        } catch (Throwable e) {
            Log.e(TAG, "Getting resolution of image failed", e);

        }

        return null;
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
}
