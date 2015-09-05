package FilesManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.text.DecimalFormat;
import java.util.Arrays;

import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;


/**
 * Exposes various methods that manipulate and validate files and their properties.
 * When instantiated, it will hold a specific file which all non-static methods operations will relate to.
 * It also defines the maximum file size allowed in the system.
 * @author Mor
 *
 */
public class FileManager {

    private static final String[] imageFormats = { "jpg", "png", "jpeg", "bmp", "gif", "tiff" };
    private static final String[] audioFormats = { "mp3", "ogg" };
    private static final String[] videoFormats = { "avi", "mpeg", "mp4" };
    private File _file;
	private String _extension;
	public static final int MAX_FILE_SIZE = 5242880; // 5MB
	private FileType fileType;

    public enum FileType { IMAGE, VIDEO, RINGTONE }


    /**
     *
     * @param filePath - Receives the full path to the file to manage
     * @throws NullPointerException - Thrown if path to file is null
     */
	public FileManager(String filePath) throws NullPointerException,FileInvalidFormatException,FileExceedsMaxSizeException, FileDoesNotExistException {
		
		if(filePath!=null)
		{
			_file = new File(filePath);
            if(doesFileExist()) {
                validateFileSize();
                _extension = extractExtension(filePath);
                fileType = validateFileFormat();
            }
            else
                throw new FileDoesNotExistException("File does not exist:"+filePath);
		}
		else
			throw new NullPointerException("The file path is null");
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


	public byte[] getFileData() throws IOException {
		
 		 // Reading the file from the disk
 		 FileInputStream fis = new FileInputStream(_file);
		 BufferedInputStream bis = new BufferedInputStream(fis);
 		 byte[] fileData = new byte[(int)_file.length()];	  				 		 
 		 bis.read(fileData);
 		 bis.close();
 		 return fileData;
	}

    public String getFileFullPath() {
        return _file.getAbsolutePath();
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

	public String getExtension() {
		
		return _extension;
	}

    /**
     *
     * @return FileType - The type of the file of the supported formats: VIDEO/RINGTONE/IMAGE
     */
	public FileType getFileType() {

           return fileType;
    }

    public static FileType getFileType(File file) throws FileInvalidFormatException {

        String extension = extractExtension(file.getAbsolutePath());
        if(Arrays.asList(imageFormats).contains(extension))
            return FileType.IMAGE;
        else if(Arrays.asList(audioFormats).contains(extension))
            return FileType.RINGTONE;
        else if (Arrays.asList(videoFormats).contains(extension))
            return FileType.VIDEO;

        delete(file);

        throw new FileInvalidFormatException(extension);
    }

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

    private static String extractExtension(String filePath) {

        String tmp_str[] = filePath.split("\\.");
        String ext = tmp_str[1];
        return ext;
    }
//    /**
//     * Deleting a directory recursively
//     * @param directory - The directory to delete
//     * @return
//     * @throws NullPointerException
//     * @throws FileNotFoundException
//     */
    /*
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
	*/

    /**
     * Allows to delete the file safely (renaming first)
     */
    public void delete() {

        final File to = new File(_file.getAbsolutePath() + System.currentTimeMillis());
        _file.renameTo(to);
        to.delete();
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


    public boolean doesFileExist() {

        return _file.exists();
    }
}
