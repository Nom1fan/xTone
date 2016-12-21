package com.utils;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.data.objects.Constants;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exceptions.FileDoesNotExistException;
import com.exceptions.FileExceedsMaxSizeException;
import com.exceptions.FileInvalidFormatException;
import com.exceptions.FileMissingExtensionException;
import com.files.media.MediaFile;

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

    public static boolean isVideoFileCorrupted(String mediaFilePath, Context context) {

        boolean fileIsCorrupted;
        try {
            MediaFile managedFile = new MediaFile(mediaFilePath);
            if(canVideoBePrepared(context, managedFile))
                fileIsCorrupted = false;
            else
            {
                fileIsCorrupted =true;
                log(Log.INFO,TAG, "Video Is Corrupted. Video File Path: " + mediaFilePath);
            }

        } catch (FileInvalidFormatException |
                FileExceedsMaxSizeException |
                FileDoesNotExistException |
                FileMissingExtensionException e) {
            e.printStackTrace();
            fileIsCorrupted = true;
            log(Log.INFO,TAG, "Video Is missing or corrupted. Video File Path: " + mediaFilePath);
        }

        return fileIsCorrupted;
    }

    public static boolean isAudioFileCorrupted(String mediaFilePath, Context context) {

        boolean fileIsCorrupted;
        try {
            MediaFile managedFile = new MediaFile(mediaFilePath);

            if(canAudioBePrepared(context, managedFile))
                fileIsCorrupted = false;
            else
            {
                fileIsCorrupted =true;
                log(Log.INFO,TAG, "Ringtone Is Corrupted. Ringtone file path: " + mediaFilePath);
            }

        } catch (FileInvalidFormatException |
                FileExceedsMaxSizeException |
                FileDoesNotExistException |
                FileMissingExtensionException e) {
            e.printStackTrace();
            fileIsCorrupted = true;
            log(Log.INFO,TAG, "Ringtone Is missing or corrupted. Ringtone File Path: " + mediaFilePath);
        }

        return fileIsCorrupted;
    }

    public static boolean isValidImageFormat(String pathOrUrl) {
        String extension = pathOrUrl.substring(pathOrUrl.lastIndexOf(".") + 1);
        return imageFormatsList.contains(extension.toLowerCase());
    }

    public static boolean isValidAudioFormat(String pathOrUrl) {
        String extension = pathOrUrl.substring(pathOrUrl.lastIndexOf(".") + 1);
        return audioFormatsList.contains(extension.toLowerCase());
    }

    public static boolean isValidVideoFormat(String pathOrUrl) {
        String extension = pathOrUrl.substring(pathOrUrl.lastIndexOf(".") + 1);
        return videoFormatsList.contains(extension.toLowerCase());
    }

    public static boolean doesFileExistInHistoryFolderByMD5(String md5,String folder) {
        boolean result = false;
        File yourDir = new File(folder);
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

    public static void triggerMediaScanOnFile(Context context, File file) {
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
    }

    public static Set<String> getAllAudioHistoryFiles() {
        File parentDir = new File(Constants.AUDIO_HISTORY_FOLDER);

        Set<String> inFiles = new HashSet<String>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (!file.isDirectory()) {

                inFiles.add(file.getAbsolutePath());
            }
        }
        return inFiles;
    }


    /**
     * Retrieves the file duration using MediaMetadataRetriever
     *
     * @param managedFile The file to retrieve its duration
     * @return The file duration in seconds
     * @see MediaMetadataRetriever
     */
    public static long getFileDurationInSeconds(Context context, MediaFile managedFile) {
        return getFileDurationInMilliSeconds(context, managedFile) / 1000;
    }

    /**
     * Retrieves the file duration using MediaMetadataRetriever
     *
     * @param managedFile The file to retrieve its duration
     * @return The file duration in milliseconds
     * @see MediaMetadataRetriever
     */
    public static long getFileDurationInMilliSeconds(Context context, MediaFile managedFile) {

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, Uri.fromFile(managedFile.getFile()));
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long timeInMilli = Long.parseLong(time);
        return timeInMilli;
    }

    public static MediaFile createMediaFile(File file) {
        MediaFile result = null;
        try {
            result = new MediaFile(file);
        }
        catch(Exception e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, e.getMessage());
        }
        return result;
    }

    public static String getFileNameByUrl(String url) {
        return FilenameUtils.getName(url).replaceAll("%20"," ");
    }

    public static String getFileNameWithoutExtensionByUrl(String url) {
        return FilenameUtils.getBaseName(url).replaceAll("%20"," ");
    }

    private static boolean canVideoBePrepared(Context ctx, MediaFile managedFile) {

        boolean result = true;
        try {
            MediaFile.FileType fType = managedFile.getFileType();
            String filepath = managedFile.getFileFullPath();
            final File root = new File(filepath);
            Uri uri = Uri.fromFile(root);

            switch (fType) {
                case VIDEO:
                    checkIfWeCanPrepareVideo(ctx, uri);
                    break;

                case IMAGE:
                    break;
            }
        } catch (Exception e) {
            result = false;
        }
        return result;

    }

    private static boolean canAudioBePrepared(Context ctx, MediaFile managedFile) {

        boolean result = true;
        try {
            MediaFile.FileType fType = managedFile.getFileType();
            String filepath = managedFile.getFileFullPath();
            final File root = new File(filepath);
            Uri uri = Uri.fromFile(root);

            switch (fType) {
                case AUDIO:
                    checkIfWeCanPrepareSound(ctx, uri);
                    break;
            }
        } catch (Exception e) {
            result = false;
        }
        return result;

    }

    private static void checkIfWeCanPrepareSound(Context ctx, Uri audioUri) throws IOException {

        Crashlytics.log(Log.INFO,TAG, "Checking if Sound Can Be Prepared and work");
        MediaPlayer mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setDataSource(ctx, audioUri);
        mMediaPlayer.prepare();
        mMediaPlayer.setLooping(true);
    }

    private static void checkIfWeCanPrepareVideo(Context ctx, Uri videoUri) throws Exception {

        Crashlytics.log(Log.INFO,TAG, "Checking if Video Can Be Prepared and work");
        MediaPlayer mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setDataSource(ctx, videoUri);
        mMediaPlayer.prepare();
        mMediaPlayer.setLooping(true);
        int width = mMediaPlayer.getVideoWidth();
        int height = mMediaPlayer.getVideoHeight();
        if (width <= 0 || height <= 0) {
            throw new Exception();
        }
    }
}
