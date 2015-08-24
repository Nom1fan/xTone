package FilesManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;

import Exceptions.FileExceedsMaxSizeException;


/**
 * Exposes various methods that manipulate and validate files and their properties.
 * When instantiated, it will hold a specific file which all non-static methods operations will relate to.
 * It also defines the maximum file size allowed in the system.
 * @author Mor
 *
 */
public class FileManager {
	
	private File _file;
	private String _extension;
	public static final int MAX_FILE_SIZE = 5242880; // 5MB
	
	public FileManager(String filePath) throws NullPointerException {
		
		if(filePath!=null)
		{
			_file = new File(filePath);
			String[] tmp_arr = filePath.split("\\.");					
			_extension = tmp_arr[1];
		}
		else
			throw new NullPointerException("The file path is null");
	}
			
	public void validateFileSize() throws FileExceedsMaxSizeException {
		
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
}
