package com.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.data_objects.Constants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import FilesManager.FileManager;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 01/07/2016.
 */
public abstract class MediaFilesUtils {

    private static final String TAG = MediaFilesUtils.class.getSimpleName();

    private static final String[] imageFormats = { "jpg", "png", "jpeg", "bmp", "gif" , "webp" };
    private static final List<String> imageFormatsList = Arrays.asList(imageFormats);
    private static final String[] audioFormats = { "mp3", "ogg" , "flac" , "mid" , "xmf" , "mxmf" , "rtx" , "ota" , "imy" , "wav" ,"m4a" , "aac"};
    private static final List<String> audioFormatsList = Arrays.asList(audioFormats);
    private static final String[] videoFormats = { "avi", "mpeg", "mp4", "3gp", "wmv" , "webm" , "mkv"  };
    private static final List<String> videoFormatsList = Arrays.asList(videoFormats);

    public static boolean doesFileExistInHistoryFolderByMD5(String md5) {
        boolean result = false;
        File yourDir = new File(Constants.HISTORY_FOLDER);
        for (File f : yourDir.listFiles()) {

            if (f.isFile()) {
                String fileName = f.getName();
                if (fileName.contains(md5)) {
                    result = true;
                    break;
                }
            }
        }
        return result;

    }

    public static File getFileByMD5(String md5, String folderPath) {
        File result = null;
        File yourDir = new File(folderPath);
        for (File f : yourDir.listFiles()) {

            if (f.isFile()) {
                String fileName = f.getName();
                if (fileName.contains(md5)) {
                    result = f;
                    break;
                }
            }
        }
        return result;
    }

    public static boolean doesFileExistInFolder(String folder, String targetFileName) {
        boolean result = false;
        File yourDir = new File(folder);
        for (File f : yourDir.listFiles()) {

            if (f.isFile()) {
                String fileName = f.getName();
                if (fileName.contains(targetFileName)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public static String constructOutputFilePath(FileManager file, String procAction, String outPath) {
        String currentDateTimeString = new SimpleDateFormat("dd_MM_yy_HHmmss").format(new Date());

        // Givin a unique name to the file to make sure there won't be any duplicates
        return outPath + currentDateTimeString + "_" + file.getMd5() + procAction + "." + file.getFileExtension();
    }

    public static void triggerMediaScanOnFile(Context context, File file) {
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
    }

    public static FileManager createMediaFile(File file) {
        FileManager result = null;
        try {
            result =  new FileManager(file);
        }
        catch(Exception e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, e.getMessage());
        }
        return result;
    }
}
