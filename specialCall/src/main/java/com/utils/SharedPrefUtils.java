package com.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefUtils {

	/* Shared pref names */
	public static final String GENERAL = "General";
	public static final String SERVER_PROXY = "LogicServerProxyService";
	public static final String UPLOADED_MEDIA_THUMBNAIL = "UploadedMediaThumbnail";
	public static final String WAS_RINGTONE_UPLOADED ="WasRingToneUploaded";
	public static final String MEDIA_FILEPATH = "Media";
	public static final String RINGTONE_FILEPATH = "ringToneFilePath";


	/* Shared pref keys under GENERAL */
	public static final String DESTINATION_NUMBER = "DestinationNumber";
	public static final String DESTINATION_NAME = "DestinationName";
	public static final String MY_NUMBER = "MyPhoneNumber";
	public static final String APP_STATE = "AppState";
	public static final String LOADING_MESSAGE = "LoadingMessage";
	public static final String MY_DEVICE_TOKEN = "MyDeviceToken";
	public static final String WAS_SPIMAGE_DECODED ="WasSpImageDecoded";

	/* Shared pref keys under SERVER_PROXY */
	public static final String RECONNECT_INTERVAL = "LogicServerProxyService.RECONNECT_INTERVAL";


	public static int getInt(Context context, String prefsName, String key){
		SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		return prefs.getInt(key, 0);
	}
	
	public static int getInt(Context context, String prefsName, String key, int DefaultValue){
		SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		return prefs.getInt(key, DefaultValue);
	}

	public static Long getLong(Context context, String prefsName, String key, Long DefaultValue){
		SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		return prefs.getLong(key, DefaultValue);
	}
	
	public static String getString(Context context, String prefsName, String key){
		SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		return prefs.getString(key, "");
	}
	
	public static Boolean getBoolean(Context context, String prefsName, String key){
		SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		return prefs.getBoolean(key, false);
	}
	
	public static void setInt(Context context, String prefsName, String key, int value){
		SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		prefs.edit().putInt(key, value).apply();
	}

	public static void setLong(Context context, String prefsName, String key, Long value){
		SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		prefs.edit().putLong(key, value).apply();
	}
	
	public static void setString(Context context, String prefsName, String key, String value){
		SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		prefs.edit().putString(key, value).apply();
	}
	
	public static void setBoolean(Context context, String prefsName, String key, Boolean value){
		SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		prefs.edit().putBoolean(key, value).apply();
	}

	public static void remove(Context context, String prefsName, String key) {
		SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		prefs.edit().remove(key).apply();
	}
	
	
}
