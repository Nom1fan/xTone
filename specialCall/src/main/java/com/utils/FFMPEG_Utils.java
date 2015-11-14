package com.utils;

import android.content.Context;
import android.util.Log;

import com.data_objects.Constants;
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
public abstract class FFMPEG_Utils {

    private static final String TAG = FFMPEG_Utils.class.getSimpleName();
    private static final String workFolder = Constants.specialCallOutgoingPath;
    private static final HashMap<String,String> extension2vCodec = new HashMap(){{
        put("mp4", "mpeg4");

    }};


    /**
     * Compresses a video file if possible. Otherwise, returns the base (untouched) file.
     * @param baseFile The base file to compress
     * @param outPath  The path of the compressed file
     * @param context
     * @return
     */
    public static FileManager compressVideoFile(FileManager baseFile, String outPath, Context context) {

        String extension = baseFile.getFileExtension();
        String vCodec = extension2vCodec.get(extension);
        File compressedFile = new File(outPath + "/" + baseFile.getNameWithoutExtension() + "_comp." + extension);
        if(compressedFile.exists())
            FileManager.delete(compressedFile);

        try {
            LoadJNI vk = new LoadJNI();

            String[] complexCommand =
                    {"ffmpeg", "-y", "-i", baseFile.getFileFullPath(), "-strict", "experimental", "-s", "320x240",
                            "-r", "25", "-vcodec", vCodec, "-b", "150k", "-ab", "48000", "-ac", "2", "-ar", "22050",
                            compressedFile.getAbsolutePath()};

            vk.run(complexCommand, workFolder, context);
            return new FileManager(compressedFile);

        }
        catch(Throwable e) {
            Log.e(TAG, "Compressing video file failed", e);
        }

        // Could not compress, returning uncompressed (untouched) file
        return baseFile;
    }

    /**
     * Resizes an image file resolution by 30%, maintaining aspect ratio
     * @param baseFile The image file to compress
     * @param outPath The output of the compressed file
     * @param width - The width parameter of the original resolution
     * @param context
     * @return
     */
    public static FileManager compressImageFile(FileManager baseFile, String outPath, double width, Context context) {

        String extension = baseFile.getFileExtension();
        File compressedFile = new File(outPath + "/" + baseFile.getNameWithoutExtension() + "_comp." + extension);
        if(compressedFile.exists())
            FileManager.delete(compressedFile);

        try {
            LoadJNI vk = new LoadJNI();
            double percent = 0.7;
            width = width * percent;

            String[] complexCommand =
                    {"ffmpeg", "-i", baseFile.getFileFullPath(), "-vf", "scale="+(int)width+":-1", compressedFile.getAbsolutePath()};

            vk.run(complexCommand, workFolder, context);
            return new FileManager(compressedFile);

        }
        catch(Throwable e) {
            Log.e(TAG, "Compressing image file failed", e);
        }

        // Could not compress, returning uncompressed (untouched) file
        return baseFile;
    }

    /**
     * Returns the duration of a video/audio file in seconds. Returns 0 for any other file type.
     * @param managedFile
     * @param context
     * @return
     */
    public static long getFileDurationInSeconds(FileManager managedFile, Context context) {

        try {
            LoadJNI vk = new LoadJNI();
            FileManager.FileType fType = managedFile.getFileType();
            if (fType.equals(FileManager.FileType.RINGTONE) ||
                fType.equals(FileManager.FileType.VIDEO)) {

                    String[] complexCommand =
                            {"ffmpeg", "-i", managedFile.getFileFullPath() };
                    vk.run(complexCommand, workFolder, context);
                    BufferedReader br = new BufferedReader(new FileReader(workFolder+"vk.log"));
                    String line = "";
                    boolean cont = true;

                    while(cont && (line = br.readLine())!=null) {
                        if(line.contains("Duration"))
                            cont = false;
                    }
                    String unformattedDuration = line.substring(12,20);
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                    Date reference = dateFormat.parse("00:00:00");
                    Date date = dateFormat.parse(unformattedDuration);
                    long seconds = (date.getTime() - reference.getTime()) / 1000L;
                    return seconds;
            }

        }
        catch(Throwable e) {
            Log.e(TAG, "Getting duration of file failed", e);
        }

        return 0;

    }

    /**
     * Trims a video/audio file from 0 seconds to endTime seconds, without re-encoding.
     * @param managedFile - Video/audio file to trim
     * @param outPath - The path of the trimmed video/audio
     * @param endTime - The time to end the cut in
     * @param context
     * @return
     */
    public static FileManager trim(FileManager managedFile, String outPath ,Double endTime, Context context) {

        String extension = managedFile.getFileExtension();
        File trimmedFile = new File(outPath + "/" + managedFile.getNameWithoutExtension() + "_trimmed." + extension);
        if(trimmedFile.exists())
            FileManager.delete(trimmedFile);

        try {
            LoadJNI vk = new LoadJNI();

            String[] complexCommand =
                    {"ffmpeg", "-i", managedFile.getFileFullPath(), "-vcodec", "copy", "-acodec",
                            "copy" , "-ss", "0", "-t" , endTime.toString(), trimmedFile.getAbsolutePath() };

            vk.run(complexCommand, workFolder, context);
            return new FileManager(trimmedFile);

        }
        catch(Throwable e) {
            Log.e(TAG, "Trimming file failed", e);
        }
        // Could not trim, returning untrimmed (untouched) file
        return managedFile;

    }

    public static int[] getImageResolution(FileManager managedFile, Context context) {

        try {
            LoadJNI vk = new LoadJNI();
            FileManager.FileType fType = managedFile.getFileType();
            if (fType.equals(FileManager.FileType.IMAGE)) {

                String[] complexCommand =
                        {"ffmpeg", "-i", managedFile.getFileFullPath()};
                vk.run(complexCommand, workFolder, context);
                BufferedReader br = new BufferedReader(new FileReader(workFolder + "vk.log"));
                String line = "";
                boolean cont = true;

                while(cont && (line = br.readLine())!=null) {
                    if(line.contains("sof0"))
                        cont = false;
                }

                int pivot_index = line.lastIndexOf(" ")+1;
                String unformattedResolution = line.substring(pivot_index, line.length());
                String[] sArrayTmp = unformattedResolution.split("x");
                int[] iArraytmp = new int[2];
                iArraytmp[0] = Integer.parseInt(sArrayTmp[0]);
                iArraytmp[1] = Integer.parseInt(sArrayTmp[1]);

                return iArraytmp;
//                while (cont && (line = br.readLine()) != null) {
//                    if (line.contains("Duration"))
//                        cont = false;
//                }
            }
        }
        catch(Throwable e) {
            Log.e(TAG, "Getting resolution of image failed", e);

        }

        return null;
    }
}
