package FilesManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Arrays;

import DataObjects.SharedConstants;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;


/**
 * Exposes various methods that manipulate and validate files and their properties.
 * When instantiated, it will hold a specific file which all non-static methods operations will relate to.
 * It also defines the maximum file size allowed in the system.
 * @author Mor
 *
 */
public class FileManager implements Serializable {

    private static final long serialVersionUID = -6478414954653475111L;
    private static final String[] imageFormats = { "jpg", "png", "jpeg", "bmp", "gif", "tiff" };
    private static final String[] audioFormats = { "mp3", "ogg" };
    private static final String[] videoFormats = { "avi", "mpeg", "mp4", "3gp", "wmv" };

    private File _file;
    private String _extension;
    private long _size;
    private FileType _fileType;
    private String _uncompdFileFullPath;
    private boolean isCompressed = false;

    public static final int MAX_FILE_SIZE = 16777216; // 16MB

    public enum FileType { IMAGE, VIDEO, RINGTONE }


    /**
     * Constructor
     * @param file - The file to manage
     * @throws NullPointerException
     * @throws FileInvalidFormatException
     * @throws FileExceedsMaxSizeException
     * @throws FileDoesNotExistException
     * @throws FileMissingExtensionException
     */
    public FileManager(File file) throws NullPointerException,FileInvalidFormatException,FileExceedsMaxSizeException, FileDoesNotExistException, FileMissingExtensionException {

        if(file!=null)
        {
            _file = file;
            if(doesFileExist()) {
                validateFileSize();
                _extension = extractExtension(_file.getAbsolutePath());
                _fileType = validateFileFormat();
                _size = _file.length();
            }
            else
                throw new FileDoesNotExistException("File does not exist:"+_file.getAbsolutePath());
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
    public FileManager(String filePath) throws NullPointerException,FileInvalidFormatException,FileExceedsMaxSizeException, FileDoesNotExistException, FileMissingExtensionException {

        if(filePath!=null)
        {
            _file = new File(filePath);
            if(doesFileExist()) {
                validateFileSize();
                _extension = extractExtension(filePath);
                _fileType = validateFileFormat();
                _size = (int) _file.length();
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

        return _file.getName().split("\\.")[0];
    }

    public File getFile() {
        return _file;
    }

    public byte[] getFileData() throws IOException {

        // Reading the file from the disk
        FileInputStream fis = new FileInputStream(_file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        byte[] fileData = new byte[(int)_file.length()];
        bis.read(fileData);
        bis.close();
        return fileData;
    }

    public String getMd5() {

        return FileManager.getMD5(getFileFullPath());
    }

    /**
     * @return returns the original path of the file before compression
     */
    public String getFileFullPath() {

        if(isCompressed)
            return _uncompdFileFullPath;
        return _file.getAbsolutePath();
    }

    public String getFileExtension() {

        return _extension;
    }

    public String get_uncompdFileFullPath() {
        return _uncompdFileFullPath;
    }

    public void set_uncompdFileFullPath(String _uncompdFileFullPath) {
        this._uncompdFileFullPath = _uncompdFileFullPath;
    }

    public long getFileSize() {
        return _size;
    }

    /**
     * Delete the file safely (rename first)
     */
    public void delete() {

        final File to = new File(_file.getAbsolutePath() + System.currentTimeMillis());
        _file.renameTo(to);
        to.delete();
    }

    public boolean doesFileExist() {

        return _file.exists();
    }

    /**
     *
     * @return FileType - The type of the file of the supported formats: VIDEO/RINGTONE/IMAGE
     */
    public FileType getFileType() {

        return _fileType;
    }
    //endregion

    //region Private instance methods
    /**
     * Validates if the file is among the valid file formats
     * @throws FileInvalidFormatException - Thrown if the file is not found in valid file formats lists
     * @return FileType - The valid file type found
     */
    private FileType validateFileFormat() throws FileInvalidFormatException {

        if(Arrays.asList(imageFormats).contains(_extension))
            return FileType.IMAGE;
        else if(Arrays.asList(audioFormats).contains(_extension))
            return FileType.RINGTONE;
        else if (Arrays.asList(videoFormats).contains(_extension))
            return FileType.VIDEO;

        delete();

        throw new FileInvalidFormatException(_extension);
    }

    /**
     * Validates the the file size does not exceeds FileManager.MAX_FILE_SIZE
     * @throws FileExceedsMaxSizeException - Thrown if file size exceeds FileManager.MAX_FILE_SIZE
     */
    private void validateFileSize() throws FileExceedsMaxSizeException {

        long fileSize = _file.length();
        if(fileSize >= MAX_FILE_SIZE)
            throw new FileExceedsMaxSizeException();
    }
    //endregion

    //region Public static methods
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

    public static FileType getFileType(String filePath) throws FileInvalidFormatException, FileDoesNotExistException, FileMissingExtensionException {

        File file = new File(filePath);
        if(file.exists()) {
            String extension = extractExtension(filePath);
            if (Arrays.asList(imageFormats).contains(extension))
                return FileType.IMAGE;
            else if (Arrays.asList(audioFormats).contains(extension))
                return FileType.RINGTONE;
            else if (Arrays.asList(videoFormats).contains(extension))
                return FileType.VIDEO;
            else
            {
                delete(file);
                throw new FileInvalidFormatException(extension);
            }
        }
        else
            throw new FileDoesNotExistException("File does not exist:"+file.getAbsolutePath());

    }

    public static FileType getFileType(File file) throws FileInvalidFormatException, FileDoesNotExistException, FileMissingExtensionException {


        if(file.exists()) {
            String extension = extractExtension(file.getAbsolutePath());
            if (Arrays.asList(imageFormats).contains(extension))
                return FileType.IMAGE;
            else if (Arrays.asList(audioFormats).contains(extension))
                return FileType.RINGTONE;
            else if (Arrays.asList(videoFormats).contains(extension))
                return FileType.VIDEO;
            else
            {
                delete(file);
                throw new FileInvalidFormatException(extension);
            }
        }
        else
            throw new FileDoesNotExistException("File does not exist:"+file.getAbsolutePath());


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

    public static String extractExtension(String filePath) throws FileMissingExtensionException{

        String tmp_str[] = filePath.split("\\.");
        if(tmp_str.length<2)
            throw new FileMissingExtensionException("File is missing extension:"+filePath);
        String ext = tmp_str[1];
        return ext;
    }
    //endregion

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        builder.
                append("Filename:").append(getFileNameWithExtension(getFileFullPath())).append(", ").
                append("FileSize:").append(_size).append(", ").
                append("FileType:").append(_fileType).append(", ").
                append("IsCompressed:").append(isCompressed);

        return builder.toString();
    }




}
