package com.files.media;

import com.exceptions.FileDoesNotExistException;
import com.exceptions.FileExceedsMaxSizeException;
import com.exceptions.FileInvalidFormatException;
import com.exceptions.FileMissingExtensionException;
import com.utils.MediaFilesUtils;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Arrays;

import static com.utils.MediaFilesUtils.extractExtension;
import static com.utils.MediaFilesUtils.getFileNameWithExtension;


/**
 * Exposes various methods that manipulate and validate files and their properties.
 * When instantiated, it will hold a specific file which all non-static methods operations will relate to.
 * It also defines the maximum file size allowed in the system.
 * @author Mor
 *
 */
public class MediaFile implements Serializable {

    private static final long serialVersionUID = -6478414954653475111L;
    private static final String[] imageFormats = { "jpg", "png", "jpeg", "bmp", "gif" , "webp" };
    private static final String[] audioFormats = { "mp3", "ogg" , "flac" , "mid" , "xmf" , "mxmf" , "rtx" , "ota" , "imy" , "wav" ,"m4a" , "aac", "amr"};
    private static final String[] videoFormats = { "mp4", "3gp", "webm" , "mkv"  };

    private String md5;
    private File file;
    private String extension;
    private long size;
    private FileType fileType;
    private String uncompdFileFullPath;
    private boolean isCompressed = false;

    public static final int MAX_FILE_SIZE = 16777216; // 16MB

    public enum FileType { IMAGE, VIDEO, AUDIO }

    public MediaFile() {

    }

    /**
     * Constructor
     * @param file - The file to manage
     * @throws NullPointerException
     * @throws FileInvalidFormatException
     * @throws FileExceedsMaxSizeException
     * @throws FileDoesNotExistException
     * @throws FileMissingExtensionException
     */
    public MediaFile(File file) throws NullPointerException,FileInvalidFormatException,FileExceedsMaxSizeException, FileDoesNotExistException, FileMissingExtensionException {

        if(file!=null)
        {
            this.file = file;
            if(doesFileExist()) {
                validateFileSize();
                extension = extractExtension(this.file.getAbsolutePath());
                fileType = validateFileFormat();
                size = this.file.length();
                md5 = MediaFilesUtils.getMD5(getFileFullPath());
            }
            else
                throw new FileDoesNotExistException("File does not exist:"+ this.file.getAbsolutePath());
        }
        else
            throw new NullPointerException("The file is null");
    }

    /**
     * Constructor
     * @param filePath - The path to the file to manage
     * @throws NullPointerException
     * @throws FileInvalidFormatException
     * @throws FileExceedsMaxSizeException
     * @throws FileDoesNotExistException
     * @throws FileMissingExtensionException
     */
    public MediaFile(String filePath) throws NullPointerException,FileInvalidFormatException,FileExceedsMaxSizeException, FileDoesNotExistException, FileMissingExtensionException {

        if(filePath!=null)
        {
            file = new File(filePath);
            if(doesFileExist()) {
                validateFileSize();
                extension = extractExtension(filePath);
                fileType = validateFileFormat();
                size = (int) file.length();
                md5 = MediaFilesUtils.getMD5(getFileFullPath());
            }
            else
                throw new FileDoesNotExistException("File does not exist:"+filePath);
        }
        else
            throw new NullPointerException("The file path is null");
    }

    //region Public instance methods
    public boolean isCompressed() {
        return isCompressed;
    }

    public void setIsCompressed(boolean isCompressed) {
        this.isCompressed = isCompressed;
    }

    public String getNameWithoutExtension() {

        return file.getName().split("\\.")[0];
    }

    public File getFile() {
        return file;
    }

    public byte[] getFileData() throws IOException {

        // Reading the file from the disk
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        byte[] fileData = new byte[(int) file.length()];
        bis.read(fileData);
        bis.close();
        return fileData;
    }

    public String getMd5() {
        return md5;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    /**
     * @return returns the original path of the file before compression
     */
    public String getFileFullPath() {

        if(isCompressed)
            return uncompdFileFullPath;
        return file.getAbsolutePath();
    }

    /**
     * @return returns the file path of the compressed file if exists. Otherwise, the regular file path.
     */
    public String getCompFileFullPath() {

        return file.getAbsolutePath();
    }

    public String getFileExtension() {

        return extension;
    }

    public String getUncompdFileFullPath() {
        return uncompdFileFullPath;
    }

    public void setUncompdFileFullPath(String uncompdFileFullPath) {
        this.uncompdFileFullPath = uncompdFileFullPath;
    }

    public long getFileSize() {
        return size;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    /**
     * Delete the file safely (rename first)
     */
    public void delete() {

        final File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
        file.renameTo(to);
        to.delete();
    }

    public boolean doesFileExist() {

        return file.exists();
    }

    /**
     *
     * @return FileType - The type of the file of the supported formats: VIDEO/AUDIO/IMAGE
     */
    public FileType getFileType() {

        return fileType;
    }
    //endregion

    //region Private instance methods
    /**
     * Validates if the file is among the valid file formats
     * @throws FileInvalidFormatException - Thrown if the file is not found in valid file formats lists
     * @return FileType - The valid file type found
     */
    private FileType validateFileFormat() throws FileInvalidFormatException {

        if(Arrays.asList(imageFormats).contains(extension))
            return FileType.IMAGE;
        else if(Arrays.asList(audioFormats).contains(extension))
            return FileType.AUDIO;
        else if (Arrays.asList(videoFormats).contains(extension))
            return FileType.VIDEO;

        delete();

        throw new FileInvalidFormatException(extension);
    }

    /**
     * Validates the the file size does not exceeds MediaFile.MAX_FILE_SIZE
     * @throws FileExceedsMaxSizeException - Thrown if file size exceeds MediaFile.MAX_FILE_SIZE
     */
    private void validateFileSize() throws FileExceedsMaxSizeException {

        long fileSize = file.length();
        if(fileSize >= MAX_FILE_SIZE)
            throw new FileExceedsMaxSizeException();
    }
    //endregion


    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        builder.
                append("Filename:").append(getFileNameWithExtension(getFileFullPath())).append(", ").
                append("FileSize:").append(size).append(", ").
                append("FileType:").append(fileType).append(", ").
                append("IsCompressed:").append(isCompressed);

        return builder.toString();
    }




}
