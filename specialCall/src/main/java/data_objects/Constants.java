package data_objects;

import DataObjects.SharedConstants;
import android.os.Environment;

public class Constants {

	public static final String specialCallPath = setSpeciallCallPath();
	public static String DEVICE_TOKEN;
	
	private static String setSpeciallCallPath() {
		
		String path = Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/";
		SharedConstants.specialCallPath = path;
		return path;
	}

}
