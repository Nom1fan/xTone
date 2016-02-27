package com.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public abstract class SharedPrefUtils {

	//region Shared prefs names
	public static final String GENERAL = "General";
	public static final String SETTINGS = "Settings";
	public static final String RADIO_BUTTON_SETTINGS = "RadioButtonsSettings";
	public static final String SERVER_PROXY = "AbstractServerProxy";
	public static final String UPLOADED_CALLER_MEDIA_THUMBNAIL = "UploadedCallerMediaThumbnail";
	public static final String UPLOADED_RINGTONE_PATH ="UploadedRingTonePath";
	public static final String UPLOADED_PROFILE_MEDIA_THUMBNAIL = "UploadedProfileMediaThumbnail";
	public static final String UPLOADED_FUNTONE_PATH = "UploadedFunTonePath";
	public static final String CALLER_MEDIA_FILEPATH = "CallerMediaFilePath";
	public static final String RINGTONE_FILEPATH = "RingToneFilePath";
    public static final String PROFILE_MEDIA_FILEPATH = "ProfileMediaFilePath";
    public static final String FUNTONE_FILEPATH = "FunToneFilePath";
    //endregion

    //region Shared pref keys under GENERAL
    public static final String DESTINATION_NUMBER = "DestinationNumber";
    public static final String DESTINATION_NAME = "DestinationName";
    public static final String MY_NUMBER = "MyPhoneNumber";
    public static final String APP_STATE = "AppState";
    public static final String APP_PREV_STATE = "AppPrevState";
    public static final String LOADING_MESSAGE = "LoadingMessage";
    public static final String MY_DEVICE_BATCH_TOKEN = "MyDeviceBatchToken";
    public static final String LAST_CHECKED_NUMBER_CACHE = "LastCheckedNumberCache";
    //endregion

    //region Shared pref keys under SERVER_PROXY
    public static final String RECONNECT_INTERVAL = "LogicServerProxyService.RECONNECT_INTERVAL";
    public static final String WAS_MID_ACTION = "WasMidAction";
    //endregion


	/* shared pref keys under SETTINGS */
	public static final String WHO_CAN_MC_ME = "WhoCanMCMe";
	public static final String BLOCK_LIST = "BlockList";
    //region Shared prefs action methods
    //region Getters
    public static int getInt(Context context, String prefsName, String key) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        return prefs.getInt(key, 0);
    }

    public static int getInt(Context context, String prefsName, String key, int DefaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        return prefs.getInt(key, DefaultValue);
    }

    public static Long getLong(Context context, String prefsName, String key, Long DefaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        return prefs.getLong(key, DefaultValue);
    }

    public static String getString(Context context, String prefsName, String key) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        return prefs.getString(key, "");
    }

    public static void setStringSet(Context context, String prefsName, String key, Set<String> value){
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = prefs.edit();
		edit.clear();
		edit.putStringSet(key, value);
		edit.commit();
    }

    public static Set<String> getStringSet(Context context, String prefsName, String key){
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        Set<String> value = new HashSet<String>();
        value =  prefs.getStringSet(key, new HashSet<String>());
        return  value;
    }

    public static Boolean getBoolean(Context context, String prefsName, String key) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, false);
    }
    //endregion

    //region Setters
    public static void setInt(Context context, String prefsName, String key, int value) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().putInt(key, value).apply();
    }

    public static void setLong(Context context, String prefsName, String key, Long value) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().putLong(key, value).apply();
    }

    public static void setString(Context context, String prefsName, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }

    public static void setBoolean(Context context, String prefsName, String key, Boolean value) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key, value).apply();
    }
    //endregion

    /**
     * Removes a key from shared preferences
     * @param context The application context
     * @param prefsName The shared preference name from which to remove
     * @param key The key to remove
     */
    public static void remove(Context context, String prefsName, String key) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().remove(key).apply();
    }
    //endregion


}
