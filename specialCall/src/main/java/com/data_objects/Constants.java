package com.data_objects;

import DataObjects.SharedConstants;

import android.content.Context;
import android.os.Environment;

import com.utils.SharedPrefUtils;

public class Constants {

	public static final String APPLICATION_ID = "7CL97UlX4EtpMyRJYshNlIQ3T12EEZ0OaZWxZjvR";
	public static final String CLIENT_KEY = "7Elu6v6XVyQRzxIqnlyIG9YGyzXuh65hD42ZUqZa";
	public static final String specialCallIncomingPath = setSpeciallCallIncomingPath();
	public static final String specialCallOutgoingPath = setSpecialCallOutgoingPath();
	public static String MY_ID(Context context) { return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER); }
	public static String MY_TOKEN(Context context) { return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN); }
	
	private static String setSpeciallCallIncomingPath() {
		
		String path = Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/";
		SharedConstants.specialCallIncomingPath = path;
		return path;
	}

	private static String setSpecialCallOutgoingPath() {

		String path = Environment.getExternalStorageDirectory()+"/SpecialCallOutgoing/";
		SharedConstants.specialCallOutgoingPath = path;
		return path;
	}

}
