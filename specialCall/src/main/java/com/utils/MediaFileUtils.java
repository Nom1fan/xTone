package com.utils;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import com.data.objects.Constants;
import com.data.objects.DefaultMediaData;
import com.data.objects.PendingDownloadData;
import com.enums.SpecialMediaType;
import com.exceptions.FileExceedsMaxSizeException;
import com.exceptions.FileInvalidFormatException;
import com.files.media.MediaFile;

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


/**
 * Created by Mor on 02/06/2017.
 */

public interface MediaFileUtils extends Utility {

    int MAX_FILE_SIZE = 16777216; // 16MB

    String[] imageFormats = {"jpg", "png", "jpeg", "bmp", "gif", "webp"};
    List<String> imageFormatsList = Arrays.asList(imageFormats);
    String[] audioFormats = {"mp3", "ogg", "flac", "mid", "xmf", "mxmf", "rtx", "ota", "imy", "wav", "m4a", "aac"};
    List<String> audioFormatsList = Arrays.asList(audioFormats);
    String[] videoFormats = {"avi", "mpeg", "mp4", "3gp", "wmv", "webm", "mkv"};
    List<String> videoFormatsList = Arrays.asList(videoFormats);

    boolean isVideoFileCorrupted(String mediaFilePath, Context context);

    boolean isAudioFileCorrupted(String mediaFilePath, Context context);

    boolean canMediaBePrepared(Context ctx, MediaFile managedFile);

    boolean isValidImageFormat(String pathOrUrl);

    boolean isValidAudioFormat(String pathOrUrl);

    boolean isValidVideoFormat(String pathOrUrl);

    boolean doesFileExistInHistoryFolderByMD5(String md5, String folder);

    File getFileByMD5(String md5, String folderPath);

    long getFileCreationDateInUnixTime(MediaFile mediaFile);

    MediaFile.FileType getFileType(String filePath);

    MediaFile.FileType getFileType(File file);

    MediaFile.FileType getFileTypeByExtension(String extension);

    String extractExtension(String filePath);

    /**
     * Allows to delete a file safely (renaming first)
     *
     * @param file - The file to delete
     */
    void delete(File file);

    /**
     * Allows to delete a file safely (renaming first)
     *
     * @param mediaFile - The file to delete
     */
    void delete(MediaFile mediaFile);

    void triggerMediaScanOnFile(Context context, File file);

    Set<String> getAllAudioHistoryFiles();

    /**
     * @param _fileSize - The size of the file in bytes
     * @return - The size of the files in common unit format (KB/MB)
     */
    String getFileSizeFormat(double _fileSize);

    String getFileNameWithExtension(String filePath);

    /**
     * Return the unique MD5 for the specific file
     *
     * @param filepath
     * @return md5 string
     * @throws Exception
     */
    String getMD5(String filepath);

    /**
     * Deleting a directory recursively
     *
     * @param directory - The directory to delete
     * @return
     * @throws NullPointerException
     * @throws FileNotFoundException Deprecated - Should probably use FileUtils by apache
     */
    @Deprecated
    boolean deleteDirectory(File directory) throws NullPointerException, FileNotFoundException;

    /**
     * Deleting a directory's contents recursively
     *
     * @param directory - The directory to delete its contents
     * @return
     * @throws NullPointerException
     * @throws FileNotFoundException
     */
    void deleteDirectoryContents(File directory) throws NullPointerException, IOException;

    boolean isExtensionValid(String extension);


    /**
     * Retrieves the file duration using MediaMetadataRetriever
     *
     * @param managedFile The file to retrieve its duration
     * @return The file duration in seconds
     * @see MediaMetadataRetriever
     */
    long getFileDurationInSeconds(Context context, MediaFile managedFile);

    /**
     * Retrieves the file duration using MediaMetadataRetriever
     *
     * @param managedFile The file to retrieve its duration
     * @return The file duration in milliseconds
     * @see MediaMetadataRetriever
     */
    long getFileDurationInMilliSeconds(Context context, MediaFile managedFile);

    String getFileNameByUrl(String url);

    String getFileNameWithoutExtensionByUrl(String url);

    String resolvePathBySpecialMediaType(PendingDownloadData pendingDownloadData);

    String resolvePathBySpecialMediaType(String sourceId, SpecialMediaType specialMediaType, DefaultMediaData defaultMediaData);

    /**
     * Validates the the file size does not exceeds MediaFile.MAX_FILE_SIZE
     *
     * @throws FileExceedsMaxSizeException - Thrown if file size exceeds MediaFile.MAX_FILE_SIZE
     */
    void validateFileSize(File file) throws FileExceedsMaxSizeException;

    /**
     * Validates if the file is among the valid file formats
     *
     * @return FileType - The valid file type found
     * @throws FileInvalidFormatException - Thrown if the file is not found in valid file formats lists
     */
    MediaFile.FileType validateFileFormat(String extension) throws FileInvalidFormatException;

    String getNameWithoutExtension(File file);

    String getNameWithoutExtension(MediaFile mediaFile);

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
    void deleteFilesIfNecessary(Context context, String sharedPrefsKey, String folder, String addedFileName, MediaFile.FileType newDownloadedFileType, String source);
}
