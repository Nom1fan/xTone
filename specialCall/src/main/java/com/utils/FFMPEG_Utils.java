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
     * Compresses video/audio/image files
     * @param managedFile - The file to compress
     * @param outPath - The path of the compressed file
     * @param context
     * @return
     */
    public static FileManager compressFile(FileManager managedFile, String outPath, Context context) {

        String extension = managedFile.getFileExtension();
        String vCodec = extension2vCodec.get(extension);
        File compressedFile = new File(outPath + "/" + managedFile.getNameWithoutExtension() + "_comp." + extension);
        if(compressedFile.exists())
            FileManager.delete(compressedFile);

        try {
            LoadJNI vk = new LoadJNI();
            String[] complexCommandChosen = new String[21];
            switch (managedFile.getFileType()) {
                case VIDEO:
                    String[] complexCommand =
                            {"ffmpeg", "-y", "-i", managedFile.getFileFullPath(), "-strict", "experimental", "-s", "320x240",
                                    "-r", "25", "-vcodec", vCodec, "-b", "150k", "-ab", "48000", "-ac", "2", "-ar", "22050",
                                    compressedFile.getAbsolutePath()};
                    complexCommandChosen = complexCommand;
                    break;
                case RINGTONE:
                    //TODO See if there is a way to compress the audio formats we support
                    break;

                case IMAGE:
                    //TODO implement image compression
                    break;
            }

            vk.run(complexCommandChosen, workFolder, context);
            return new FileManager(compressedFile);

        }
        catch(Throwable e) {
            Log.e(TAG, "Compressing file failed", e);
        }
        // Could not compress, returning uncompressed (untouched) file
        return managedFile;
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
            String[] complexCommandChosen = new String[12];
            switch (managedFile.getFileType()) {
                case RINGTONE:
                case VIDEO:
                    String[] complexCommand =
                            {"ffmpeg", "-i", managedFile.getFileFullPath(), "-vcodec", "copy", "-acodec", "copy" , "-ss", "0", "-t" , endTime.toString(), trimmedFile.getAbsolutePath() };
                    complexCommandChosen = complexCommand;
                    break;
            }

            vk.run(complexCommandChosen, workFolder, context);
            return new FileManager(trimmedFile);

        }
        catch(Throwable e) {
            Log.e(TAG, "Trimming file failed", e);
        }
        // Could not compress, returning uncompressed (untouched) file
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
