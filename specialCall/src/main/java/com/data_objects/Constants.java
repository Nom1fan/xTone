package com.data_objects;

import DataObjects.SharedConstants;
import android.os.Environment;

public class Constants {

	public static final String APPLICATION_ID = "7CL97UlX4EtpMyRJYshNlIQ3T12EEZ0OaZWxZjvR";
	public static final String CLIENT_KEY = "7Elu6v6XVyQRzxIqnlyIG9YGyzXuh65hD42ZUqZa";
	public static final String specialCallPath = setSpeciallCallPath();
	
	private static String setSpeciallCallPath() {
		
		String path = Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/";
		SharedConstants.specialCallPath = path;
		return path;
	}

}
