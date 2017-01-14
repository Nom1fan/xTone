package com.utils;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.data.objects.Constants;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
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

    public static MediaFile.FileType getFileType(String filePath) throws FileInvalidFormatException, FileDoesNotExistException, FileMissingExtensionException {

        File file = new File(filePath);
        if(file.exists()) {
            String extension = extractExtension(filePath);
            if (Arrays.asList(imageFormats).contains(extension))
                return MediaFile.FileType.IMAGE;
            else if (Arrays.asList(audioFormats).contains(extension))
                return MediaFile.FileType.AUDIO;
            else if (Arrays.asList(videoFormats).contains(extension))
                return MediaFile.FileType.VIDEO;
            else
            {
                delete(file);
                throw new FileInvalidFormatException(extension);
            }
        }
        else
            throw new FileDoesNotExistException("File does not exist:"+file.getAbsolutePath());

    }

    public static MediaFile.FileType getFileType(File file) throws FileInvalidFormatException, FileDoesNotExistException, FileMissingExtensionException {


        if(file.exists()) {
            String extension = extractExtension(file.getAbsolutePath());
            if (Arrays.asList(imageFormats).contains(extension))
                return MediaFile.FileType.IMAGE;
            else if (Arrays.asList(audioFormats).contains(extension))
                return MediaFile.FileType.AUDIO;
            else if (Arrays.asList(videoFormats).contains(extension))
                return MediaFile.FileType.VIDEO;
            else
            {
                delete(file);
                throw new FileInvalidFormatException(extension);
            }
        }
        else
            throw new FileDoesNotExistException("File does not exist:"+file.getAbsolutePath());


    }

    public static MediaFile.FileType getFileTypeByExtension(String extension) throws FileInvalidFormatException {

        if (Arrays.asList(imageFormats).contains(extension))
            return MediaFile.FileType.IMAGE;
        else if (Arrays.asList(audioFormats).contains(extension))
            return MediaFile.FileType.AUDIO;
        else if (Arrays.asList(videoFormats).contains(extension))
            return MediaFile.FileType.VIDEO;
        else
            throw new FileInvalidFormatException(extension);
    }

    public static String extractExtension(String filePath) throws FileMissingExtensionException{

        String tmp_str[] = filePath.split("\\.(?=[^\\.]+$)"); // getting last
        if(tmp_str.length<2)
            throw new FileMissingExtensionException("File is missing extension:"+filePath);
        String ext = tmp_str[1];
        return ext.toLowerCase();
    }

    /**
     * Allows to delete a file safely (renaming first)
     * @param file - The file to delete
     */
    public static void delete(File file) {

        final File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
        file.renameTo(to);
        to.delete();
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
     *
     * @param _fileSize - The size of the file in bytes
     * @return - The size of the files in common unit format (KB/MB)
     */
    public static String getFileSizeFormat(double _fileSize) {

        double MB = (int)Math.pow(2, 20);
        double KB = (int)Math.pow(2, 10);
        DecimalFormat df = new DecimalFormat("#.00"); // rounding to max 2 decimal places

        if(_fileSize>=MB)
        {
            double fileSizeInMB = _fileSize/MB; // File size in MBs
            return df.format(fileSizeInMB)+"MB";
        }
        else if(_fileSize>=KB)
        {
            double fileSizeInKB = _fileSize/KB; // File size in KBs
            return df.format(fileSizeInKB)+"KB";
        }

        // File size in Bytes
        return df.format(_fileSize)+"B";
    }


    /**
     * Creates a new file on the File System from given bytes array
     * @param filePath
     * @param fileData
     * @throws IOException
     */
    public static void createNewFile(String filePath, byte[] fileData) throws IOException {

        FileOutputStream fos;
        BufferedOutputStream bos;

        // Creating file
        File newFile = new File(filePath);
        newFile.getParentFile().mkdirs();
        newFile.createNewFile();
        fos = new FileOutputStream(newFile);
        bos = new BufferedOutputStream(fos);

        // Writing file to disk
        bos.write(fileData);
        bos.flush();
        bos.close();
    }

    public static String getFileNameWithExtension(String filePath){

        String tmp_str[] = filePath.split("\\/");

        String fileName = tmp_str[tmp_str.length-1];

        return fileName;

    }

    /**
     * Return the unique MD5 for the specific file
     * @param filepath
     * @return md5 string
     * @throws Exception
     */
    public static String getMD5(String filepath) {

        try {

            InputStream input =  new FileInputStream(filepath);
            byte[] buffer = new byte[1024];

            MessageDigest hashMsgDigest = null;
            hashMsgDigest = MessageDigest.getInstance("MD5");

            int read;
            do {
                read = input.read(buffer);
                if (read > 0) {
                    hashMsgDigest.update(buffer, 0, read);
                }
            } while (read != -1);
            input.close();

            StringBuffer hexString = new StringBuffer();
            byte[] hash = hashMsgDigest.digest();

            for (int i = 0; i < hash.length; i++) {
                if ((0xff & hash[i]) < 0x10) {
                    hexString.append("0"
                            + Integer.toHexString((0xFF & hash[i])));
                } else {
                    hexString.append(Integer.toHexString(0xFF & hash[i]));
                }
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deleting a directory recursively
     * @param directory - The directory to delete
     * @return
     * @throws NullPointerException
     * @throws FileNotFoundException
     */
    public static boolean deleteDirectory(File directory) throws NullPointerException, FileNotFoundException {

        if(directory == null)
            throw new NullPointerException("The file parameter cannot be null");

        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files == null) {
                return true;
            }
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        else
            throw new FileNotFoundException();

        return(directory.delete());

    }

    /**
     * Deleting a directory's contents recursively
     * @param directory - The directory to delete its contents
     * @return
     * @throws NullPointerException
     * @throws FileNotFoundException
     */
    public static void deleteDirectoryContents(File directory) throws NullPointerException, IOException {

        FileUtils.cleanDirectory(directory);
    }

    public static boolean isExtensionValid(String extension) {

        return Arrays.asList(MediaFilesUtils.imageFormats).contains(extension) ||
                Arrays.asList(MediaFilesUtils.videoFormats).contains(extension) ||
                Arrays.asList(MediaFilesUtils.audioFormats).contains(extension);


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
