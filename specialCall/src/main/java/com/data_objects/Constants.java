package com.data_objects;

import DataObjects.SharedConstants;

import android.content.Context;
import android.os.Environment;

import com.utils.SharedPrefUtils;

public class Constants {

	public static final String APPLICATION_ID = "7CL97UlX4EtpMyRJYshNlIQ3T12EEZ0OaZWxZjvR";
	public static final String CLIENT_KEY = "7Elu6v6XVyQRzxIqnlyIG9YGyzXuh65hD42ZUqZa";
	public static final String specialCallPath = setSpeciallCallPath();
	public static String MY_ID(Context context) { return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER); }
	public static String MY_TOKEN(Context context) { return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN); }
	
	private static String setSpeciallCallPath() {
		
		String path = Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/";
		SharedConstants.specialCallPath = path;
		return path;
	}

}
