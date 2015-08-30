package data_objects;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefUtils {

	public static final String GENERAL = "general"; 
	public static final String RINGTONE = "ringtone";
	public static final String MEDIA = "media";
	public static final String RINGTONE_URI = "ringtoneUri";
	public static final String DESTINATION_NUMBER = "DestinationNumber";
	public static final String DESTINATION_NAME = "DestinationName";
	public static final String MY_NUMBER = "MyPhoneNumber";
	
	public static int getInt(Context context, String prefsName, String key){
		SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		return prefs.getInt(key, 0);
	}
	
	public static int getInt(Context context, String prefsName, String key, int DefaultValue){
		SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		return prefs.getInt(key, DefaultValue);
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
	
	public static void setString(Context context, String prefsName, String key, String value){
		SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		prefs.edit().putString(key, value).apply();
	}
	
	public static void setBoolean(Context context, String prefsName, String key, Boolean value){
		SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		prefs.edit().putBoolean(key, value).apply();
	}
	
	
}
