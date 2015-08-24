package data_objects;

import DataObjects.SharedConstants;
import android.os.Environment;

public class Constants {
	
	public static final String imageFormats[] = { "jpg", "png", "jpeg", "bmp", "gif", "tiff" };
	public static final String audioFormats[] = { "mp3", "ogg" };
	public static final String videoFormats[] = { "avi", "mpeg", "mp4" };
	public static final String specialCallPath = setSpeciallCallPath();
	
	private static String setSpeciallCallPath() {
		
		String path = Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/";
		SharedConstants.specialCallPath = path;
		return path;
	}

}
