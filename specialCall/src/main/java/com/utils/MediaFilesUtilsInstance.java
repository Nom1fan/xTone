package com.utils;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.data.objects.Constants;
import com.data.objects.DefaultMediaData;
import com.data.objects.PendingDownloadData;
import com.exceptions.FileExceedsMaxSizeException;
import com.exceptions.FileInvalidFormatException;
import com.files.media.MediaFile;
import com.logger.Logger;
import com.logger.LoggerFactory;

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
import java.util.Set;

/**
 * Created by Mor on 01/07/2016.
 */
public class MediaFilesUtilsInstance implements MediaFileUtils {

    private  final String TAG = MediaFilesUtilsInstance.class.getSimpleName();

    private Logger logger = LoggerFactory.getLogger();

    public boolean isVideoFileCorrupted(String mediaFilePath, Context context) {

        boolean fileIsCorrupted;

        MediaFile managedFile = new MediaFile(new File(mediaFilePath));
        if (canVideoBePrepared(context, managedFile))
            fileIsCorrupted = false;
        else {
            fileIsCorrupted = true;
            logger.info(TAG, "Video Is Corrupted. Video File Path: " + mediaFilePath);
        }

        return fileIsCorrupted;
    }

    public  boolean isAudioFileCorrupted(String mediaFilePath, Context context) {

        boolean fileIsCorrupted;

        MediaFile managedFile = new MediaFile(new File(mediaFilePath));

        if (canAudioBePrepared(context, managedFile))
            fileIsCorrupted = false;
        else {
            fileIsCorrupted = true;
            logger.info(TAG, "Ringtone Is Corrupted. Ringtone file path: " + mediaFilePath);
        }

        return fileIsCorrupted;
    }

    public  boolean canMediaBePrepared(Context ctx, MediaFile managedFile) {

        boolean result = true;
        try {
            MediaFile.FileType fType = managedFile.getFileType();
            String filepath = managedFile.getFile().getAbsolutePath();
            final File root = new File(filepath);
            Uri uri = Uri.fromFile(root);

            switch (fType) {
                case AUDIO:
                    checkIfWeCanPrepareSound(ctx, uri);
                    break;

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

    public  boolean isValidImageFormat(String pathOrUrl) {
        String extension = pathOrUrl.substring(pathOrUrl.lastIndexOf(".") + 1);
        return imageFormatsList.contains(extension.toLowerCase());
    }

    public  boolean isValidAudioFormat(String pathOrUrl) {
        String extension = pathOrUrl.substring(pathOrUrl.lastIndexOf(".") + 1);
        return audioFormatsList.contains(extension.toLowerCase());
    }

    public  boolean isValidVideoFormat(String pathOrUrl) {
        String extension = pathOrUrl.substring(pathOrUrl.lastIndexOf(".") + 1);
        return videoFormatsList.contains(extension.toLowerCase());
    }

    public  boolean doesFileExistInHistoryFolderByMD5(String md5, String folder) {
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

    public  File getFileByMD5(String md5, String folderPath) {
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

    public  long getFileCreationDateInUnixTime(MediaFile mediaFile) {
        long creationDateInUnixTime = 0;
        File file = mediaFile.getFile();
        if (file.exists()) {
            String name = file.getName();
            String[] split = name.split("_");
            if (split.length > 1) {
                creationDateInUnixTime = Long.valueOf(split[0]);
            }
        }
        return creationDateInUnixTime;
    }

    public  MediaFile.FileType getFileType(String filePath) {
        return getFileType(new File(filePath));
    }

    public  MediaFile.FileType getFileType(File file) {
        return getFileTypeByExtension(extractExtension(file.getAbsolutePath()));
    }

    public  MediaFile.FileType getFileTypeByExtension(String extension) {
        MediaFile.FileType fileType = null;
        if (Arrays.asList(imageFormats).contains(extension)) {
            fileType = MediaFile.FileType.IMAGE;
        } else if (Arrays.asList(audioFormats).contains(extension)) {
            fileType = MediaFile.FileType.AUDIO;
        } else if (Arrays.asList(videoFormats).contains(extension)) {
            fileType = MediaFile.FileType.VIDEO;
        }
        return fileType;
    }

    public  String extractExtension(String filePath) {
        String ext = null;
        String tmp_str[] = filePath.split("\\.(?=[^\\.]+$)"); // getting last
        if (tmp_str.length >= 2) {
            ext = tmp_str[1].toLowerCase();
        }
        return ext;
    }

    /**
     * Allows to delete a file safely (renaming first)
     *
     * @param file - The file to delete
     */
    public  void delete(File file) {

        final File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
        file.renameTo(to);
        to.delete();
    }

    /**
     * Allows to delete a file safely (renaming first)
     *
     * @param mediaFile - The file to delete
     */
    public  void delete(MediaFile mediaFile) {

        final File to = new File(mediaFile.getFile().getAbsolutePath() + System.currentTimeMillis());
        mediaFile.getFile().renameTo(to);
        to.delete();
    }

    public  void triggerMediaScanOnFile(Context context, File file) {
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
    }

    public  Set<String> getAllAudioHistoryFiles() {
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
     * @param _fileSize - The size of the file in bytes
     * @return - The size of the files in common unit format (KB/MB)
     */
    public  String getFileSizeFormat(double _fileSize) {

        double MB = (int) Math.pow(2, 20);
        double KB = (int) Math.pow(2, 10);
        DecimalFormat df = new DecimalFormat("#.00"); // rounding to max 2 decimal places

        if (_fileSize >= MB) {
            double fileSizeInMB = _fileSize / MB; // File size in MBs
            return df.format(fileSizeInMB) + "MB";
        } else if (_fileSize >= KB) {
            double fileSizeInKB = _fileSize / KB; // File size in KBs
            return df.format(fileSizeInKB) + "KB";
        }

        // File size in Bytes
        return df.format(_fileSize) + "B";
    }


    /**
     * Creates a new file on the File System from given bytes array
     *
     * @param filePath
     * @param fileData
     * @throws IOException
     */
    public  void createNewFile(String filePath, byte[] fileData) throws IOException {

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

    public  String getFileNameWithExtension(String filePath) {

        String tmp_str[] = filePath.split("\\/");

        String fileName = tmp_str[tmp_str.length - 1];

        return fileName;

    }

    /**
     * Return the unique MD5 for the specific file
     *
     * @param filepath
     * @return md5 string
     * @throws Exception
     */
    public  String getMD5(String filepath) {

        try {

            InputStream input = new FileInputStream(filepath);
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
     *
     * @param directory - The directory to delete
     * @return
     * @throws NullPointerException
     * @throws FileNotFoundException Deprecated - Should probably use FileUtils by apache
     */
    @Deprecated
    public  boolean deleteDirectory(File directory) throws NullPointerException, FileNotFoundException {

        if (directory == null)
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
        } else
            throw new FileNotFoundException();

        return (directory.delete());

    }

    /**
     * Deleting a directory's contents recursively
     *
     * @param directory - The directory to delete its contents
     * @return
     * @throws NullPointerException
     * @throws FileNotFoundException
     */
    public  void deleteDirectoryContents(File directory) throws NullPointerException, IOException {

        FileUtils.cleanDirectory(directory);
    }

    public  boolean isExtensionValid(String extension) {
        return Arrays.asList(imageFormats).contains(extension) ||
                Arrays.asList(videoFormats).contains(extension) ||
                Arrays.asList(audioFormats).contains(extension);
    }


    /**
     * Retrieves the file duration using MediaMetadataRetriever
     *
     * @param managedFile The file to retrieve its duration
     * @return The file duration in seconds
     * @see MediaMetadataRetriever
     */
    public  long getFileDurationInSeconds(Context context, MediaFile managedFile) {
        return getFileDurationInMilliSeconds(context, managedFile) / 1000;
    }

    /**
     * Retrieves the file duration using MediaMetadataRetriever
     *
     * @param managedFile The file to retrieve its duration
     * @return The file duration in milliseconds
     * @see MediaMetadataRetriever
     */
    public  long getFileDurationInMilliSeconds(Context context, MediaFile managedFile) {

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, Uri.fromFile(managedFile.getFile()));
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long timeInMilli = Long.parseLong(time);
        return timeInMilli;
    }

    public  MediaFile createMediaFile(File file) {
        MediaFile result = null;
        try {
            result = new MediaFile(file);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(TAG, e.getMessage());
        }
        return result;
    }

    public  String getFileNameByUrl(String url) {
        return FilenameUtils.getName(url).replaceAll("%20", " ");
    }

    public  String getFileNameWithoutExtensionByUrl(String url) {
        return FilenameUtils.getBaseName(url).replaceAll("%20", " ");
    }

    public  String resolvePathBySpecialMediaType(PendingDownloadData pendingDownloadData, DefaultMediaData defaultMediaData) {
        String filePath;
        String sourceId = pendingDownloadData.getSourceId();
        String extension = pendingDownloadData.getMediaFile().getExtension();
        switch (pendingDownloadData.getSpecialMediaType()) {
            case CALLER_MEDIA: {
                String fileName = sourceId + "." + extension;
                filePath = Constants.INCOMING_FOLDER + sourceId + "/" + fileName;
            }
            break;
            case PROFILE_MEDIA: {
                String fileName = sourceId + "." + extension;
                filePath = Constants.OUTGOING_FOLDER + sourceId + "/" + fileName;
            }
            break;
            case DEFAULT_CALLER_MEDIA: {
                String fileName = defaultMediaData.getDefaultMediaUnixTime() + "_" + sourceId + "." + extension;
                filePath = Constants.DEFAULT_INCOMING_FOLDER + sourceId + "/" + fileName;
            }

            break;
            case DEFAULT_PROFILE_MEDIA: {
                String fileName = defaultMediaData.getDefaultMediaUnixTime() + "_" + sourceId + "." + extension;
                filePath = Constants.DEFAULT_OUTGOING_FOLDER + sourceId + "/" + fileName;
            }
            break;
            default:
                throw new UnsupportedOperationException("Not yet implemented");

        }
        return filePath;
    }

    /**
     * Validates the the file size does not exceeds MediaFile.MAX_FILE_SIZE
     *
     * @throws FileExceedsMaxSizeException - Thrown if file size exceeds MediaFile.MAX_FILE_SIZE
     */
    public  void validateFileSize(File file) throws FileExceedsMaxSizeException {

        long fileSize = file.length();
        if (fileSize >= MAX_FILE_SIZE)
            throw new FileExceedsMaxSizeException();
    }

    /**
     * Validates if the file is among the valid file formats
     *
     * @return FileType - The valid file type found
     * @throws FileInvalidFormatException - Thrown if the file is not found in valid file formats lists
     */
    public  MediaFile.FileType validateFileFormat(String extension) throws FileInvalidFormatException {

        if (Arrays.asList(imageFormats).contains(extension))
            return MediaFile.FileType.IMAGE;
        else if (Arrays.asList(audioFormats).contains(extension))
            return MediaFile.FileType.AUDIO;
        else if (Arrays.asList(videoFormats).contains(extension))
            return MediaFile.FileType.VIDEO;

        throw new FileInvalidFormatException(extension);
    }

    public  String getNameWithoutExtension(File file) {

        return file.getName().split("\\.")[0];
    }

    public  String getNameWithoutExtension(MediaFile mediaFile) {

        return mediaFile.getFile().getName().split("\\.")[0];
    }

    /**
     * Deletes files in the source's designated directory by an algorithm based on the new downloaded file type:
     * This method does not delete the new downloaded file.
     * lets mark newDownloadedFileType as nDFT.
     * nDFT = IMAGE --> deletes images and videos
     * nDFT = AUDIO --> deletes ringtones and videos
     * nDFT = VIDEO --> deletes all
     *
     * @param context               The application context
     * @param sharedPrefsKey        The key used to remove cached file from shared prefs if needed
     * @param folder                The folder to scan the files in
     * @param addedFileName         The new downloaded file
     * @param newDownloadedFileType The type of the files just downloaded and should be created in the source designated folder
     * @param source                The source number of the sender of the file
     */
    public  void deleteFilesIfNecessary(Context context, String sharedPrefsKey, String folder, String addedFileName, MediaFile.FileType newDownloadedFileType, String source) {

        File[] files = new File(folder).listFiles();

        if (files == null)
            return;

        switch (newDownloadedFileType) {
            case AUDIO:

                for (File file : files) {
                    String fileName = file.getName(); // This includes extension
                    MediaFile.FileType fileType = getFileType(file);

                    if (!fileName.equals(addedFileName) &&
                            (fileType == MediaFile.FileType.VIDEO ||
                                    fileType == MediaFile.FileType.AUDIO)) {
                        delete(file);
                        SharedPrefUtils.remove(context, sharedPrefsKey, source);
                    }
                }
                break;
            case IMAGE:

                for (File file : files) {
                    String fileName = file.getName(); // This includes extension
                    MediaFile.FileType fileType = getFileType(file);

                    if (!fileName.equals(addedFileName) &&
                            (fileType == MediaFile.FileType.VIDEO ||
                                    fileType == MediaFile.FileType.IMAGE)) {
                        delete(file);
                    }
                }
                break;

            case VIDEO:

                for (File file : files) {
                    String fileName = file.getName(); // This includes extension
                    if (!fileName.equals(addedFileName)) {
                        delete(file);
                        SharedPrefUtils.remove(context, sharedPrefsKey, source);
                    }
                }
                break;
        }
    }

    private  boolean canVideoBePrepared(Context ctx, MediaFile managedFile) {

        boolean result = true;
        try {
            MediaFile.FileType fType = managedFile.getFileType();
            Crashlytics.log(Log.INFO, TAG, "INSIDE canVideoBePrepared files point of view");
            String filepath = managedFile.getFile().getAbsolutePath();
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
            e.printStackTrace();
            Crashlytics.log(Log.ERROR, TAG, "Failed canVideoBePrepared files exception message: " + e.getMessage());
        }
        return result;

    }

    private  boolean canAudioBePrepared(Context ctx, MediaFile managedFile) {

        boolean result = true;
        try {
            Crashlytics.log(Log.INFO, TAG, "INSIDE canAudioBePrepared files point of view");
            MediaFile.FileType fType = managedFile.getFileType();
            String filepath = managedFile.getFile().getAbsolutePath();
            final File root = new File(filepath);
            Uri uri = Uri.fromFile(root);

            switch (fType) {
                case AUDIO:
                    checkIfWeCanPrepareSound(ctx, uri);
                    break;
            }
        } catch (Exception e) {
            result = false;
            e.printStackTrace();
            Crashlytics.log(Log.ERROR, TAG, "Failed canAudioBePrepared files exception message: " + e.getMessage());
        }
        return result;

    }

    private  void checkIfWeCanPrepareSound(Context ctx, Uri audioUri) throws IOException {

        Crashlytics.log(Log.INFO, TAG, "Checking if Sound Can Be Prepared and work");
        MediaPlayer mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setDataSource(ctx, audioUri);
        mMediaPlayer.prepare();
        mMediaPlayer.setLooping(true);
        mMediaPlayer.release();
    }

    private  void checkIfWeCanPrepareVideo(Context ctx, Uri videoUri) throws Exception {

        Crashlytics.log(Log.INFO, TAG, "Checking if Video Can Be Prepared and work");
        MediaPlayer mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setDataSource(ctx, videoUri);
        mMediaPlayer.prepare();
        mMediaPlayer.setLooping(true);
        int width = mMediaPlayer.getVideoWidth();
        int height = mMediaPlayer.getVideoHeight();
        mMediaPlayer.release();
        if (width <= 0 || height <= 0) {
            throw new Exception();
        }
    }
}
