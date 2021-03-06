package com.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class SharedPrefUtils {

    //region Shared prefs names
    public static final String GENERAL                          =   "General";
    public static final String SETTINGS                         =   "Settings";
    public static final String SERVICES                         =   "Services";
    public static final String RADIO_BUTTON_SETTINGS            =   "RadioButtonsSettings";
    public static final String SERVER_PROXY                     =   "AbstractServerProxy";
    public static final String UPLOADED_CALLER_MEDIA_THUMBNAIL  =   "UploadedCallerMediaThumbnail";
    public static final String UPLOADED_RINGTONE_PATH           =   "UploadedRingTonePath";
    public static final String UPLOADED_PROFILE_MEDIA_THUMBNAIL =   "UploadedProfileMediaThumbnail";
    public static final String UPLOADED_FUNTONE_PATH            =   "UploadedFunTonePath";
    public static final String CALLER_MEDIA_FILEPATH            =   "CallerMediaFilePath";
    public static final String RINGTONE_FILEPATH                =   "RingToneFilePath";
    public static final String PROFILE_MEDIA_FILEPATH           =   "ProfileMediaFilePath";
    public static final String FUNTONE_FILEPATH                 =   "FunToneFilePath";
    public static final String SHOWCASE                         =   "Showcase";
    public static final String TRIMMED_FILES                    =   "TRIMMED_FILES";
    public static final String CONTENT_STORE                    =   "CONTENT_STORE";
    //endregion

    public static final Set<String> allSharedPrefs = new HashSet<> (Arrays.asList(

            GENERAL,
            SETTINGS,
            SERVICES,
            RADIO_BUTTON_SETTINGS,
            SERVER_PROXY,
            UPLOADED_CALLER_MEDIA_THUMBNAIL,
            UPLOADED_RINGTONE_PATH,
            UPLOADED_PROFILE_MEDIA_THUMBNAIL,
            UPLOADED_FUNTONE_PATH,
            CALLER_MEDIA_FILEPATH,
            RINGTONE_FILEPATH,
            PROFILE_MEDIA_FILEPATH,
            FUNTONE_FILEPATH,
            SHOWCASE,
            TRIMMED_FILES
    ));

    //region Shared pref keys under GENERAL
    public static final String DESTINATION_NUMBER                   =   "DestinationNumber";
    public static final String LOGIN_NUMBER                         =   "LoginNumber";
    public static final String SMS_CODE                             =   "SmsCode";
    public static final String AUTO_SMS_CODE_RECEIVED               =   "AutoSmsCodeReceived";
    public static final String AUTO_SMS_CODE                        =   "AutoSmsCode";
    public static final String DESTINATION_NAME                     =   "DestinationName";
    public static final String MY_NUMBER                            =   "MyPhoneNumber";
    public static final String APP_STATE                            =   "AppState";
    public static final String APP_PREV_STATE                       =   "AppPrevState";
    public static final String LOADING_MESSAGE                      =   "LoadingMessage";
    public static final String MY_DEVICE_FIREBASE_TOKEN =   "MyDeviceBatchToken";
    public static final String LAST_CHECKED_NUMBER_CACHE            =   "LastCheckedNumberCache";
    public static final String STRICT_MEMORY_MANAGER_DEVICES        =   "StrictMemoryManagerDevices";
    public static final String STRICT_RINGING_CAPABILITIES_DEVICES  =   "StrictRingingCapabilitiesDevices";
    public static final String ASK_BEFORE_MEDIA_SHOW                =   "AskBeforeMediaShow";
    public static final String ASK_BEFORE_MEDIA_SHOW_FOR_STANDOUT   =   "AskBeforeMediaShowForStandOut";
    public static final String ENABLE_UI_ELEMENTS_ANIMATION         =   "EnableUIElementsAnimation";
    public static final String DISABLE_UI_ELEMENTS_ANIMATION        =   "DisableUIElementsAnimation";
    public static final String DONT_SHOW_AGAIN_UPLOAD_DIALOG        =   "DontShowAgainUploadDialog";
    public static final String DONT_SHOW_AGAIN_CLEAR_DIALOG         =   "DontShowAgainClearDialog";
    public static final String DONT_SHOW_AGAIN_TIP                  =   "DontShowAgainTIP";
    public static final String DONT_SHOW_AGAIN_WINDOW_VIDEO         =   "DontShowAgainWindowVideo";
    public static final String ANDROID_VERSION                      =   "AndroidVersion";
    public static final String TIMEOUT_MSG                          =   "TIMEOUT_MSG";
    public static final String DID_APP_CRASH                        =   "DID_APP_CRASH";
    public static final String SELF_VIDEO_IMAGE_URI                 =   "OutputFileUriForVideoImageTaken";
    public static final String IMAGE_ROTATION_DEGREE                =   "ImageRotationDegree";
    public static final String AUDIO_VIDEO_START_TRIM_IN_MILISEC    =   "AudioVideoStartTrimInMiliSec";
    public static final String AUDIO_VIDEO_END_TRIM_IN_MILISEC      =   "AudioVideoEndTrimInMiliSec";
    public static final String AUDIO_HISTORY_EXIST                  =   "AUDIO_HISTORY_EXIST";
    public static final String DONT_SHOW_RESIZE_WINDOW_FOR_STANDOUT =   "dontShowfirstresizeWindow";
    //endregion

    //region Shared pref keys under CONTENT_STORE
    public static final String RESULT_FILE_PATH_FROM_CONTENT_STORE =   "RESULT_FILE_PATH_FROM_CONTENT_STORE";
    //endregion

    //region Shared pref keys under SHOWCASE
    public static final String CALL_NUMBER_VIEW                 =   "callNumberView";
    public static final String SELECT_MEDIA_VIEW                =   "SelectMediaView";
    public static final String UPLOAD_BEFORE_CALL_VIEW          =   "UploadBeforeCallView";
    //endregion

    //region Shared pref keys under SERVER_PROXY
    public static final String WAS_MID_ACTION                   =   "WAS_MID_ACTION";
    //endregion

    //region Shared pref keys under SERVICES
    public static final String OUTGOING_RINGING_SESSION                 =   "OutgoingisInRingingSession";
    public static final String INCOMING_RINGING_SESSION                 =   "IncomingisInRingingSession";
    public static final String RING_VOLUME                              =   "RingVolume";
    public static final String MUSIC_VOLUME                             =   "MusicVolume";
    public static final String RINGER_MODE                              =   "RingerMode";
    public static final String TEMP_VISUALMD5                           =   "TempVisualMd5";
    public static final String TEMP_AUDIOMD5                            =   "TempAudioMd5";
    public static final String DISABLE_VOLUME_BUTTONS                   =   "DisableVolumeButtons";

    public static final String OUTGOING_WINDOW_SESSION                  =   "OutgoingWindowSession";
    public static final String INCOMING_WINDOW_SESSION                  =   "IncomingWindowSession";
    public static final String INCOMING_MC_WINDOW_WIDTH_BY_USER         =   "IncomingMCWindowWidthByUser";
    public static final String INCOMING_MC_WINDOW_HEIGHET_BY_USER       =   "IncomingMCWindowHeighetByUser";
    public static final String OUTGOING_MC_WINDOW_WIDTH_BY_USER         =   "OutgoingMCWindowWidthByUser";
    public static final String OUTGOING_MC_WINDOW_HEIGHET_BY_USER       =   "OutgoingMCWindowHeighetByUser";
    public static final String PREVIEW_MC_WINDOW_WIDTH_BY_USER          =   "PreviewMCWindowWidthByUser";
    public static final String PREVIEW_MC_WINDOW_HEIGHET_BY_USER        =   "PreviewMCWindowHeighetByUser";
    public static final String INCOMING_MC_WINDOW_X_LOCATION_BY_USER    =   "IncomingMCWindowXLocationByUser";
    public static final String INCOMING_MC_WINDOW_Y_LOCATION_BY_USER    =   "IncomingMCWindowYLocationByUser";
    public static final String OUTGOING_MC_WINDOW_X_LOCATION_BY_USER    =   "OutgoingMCWindowXLocationByUser";
    public static final String OUTGOING_MC_WINDOW_Y_LOCATION_BY_USER    =   "OutgoingMCWindowYLocationByUser";
    public static final String PREVIEW_MC_WINDOW_X_LOCATION_BY_USER     =   "PreviewMCWindowXLocationByUser";
    public static final String PREVIEW_MC_WINDOW_Y_LOCATION_BY_USER     =   "PreviewMCWindowYLocationByUser";
    public static final String DEVICE_SCREEN_HEIGHET                    =   "DeviceScreenHeighet";
    public static final String DEVICE_SCREEN_WIDTH                      =   "DeviceScreenWidth";
    public static final String DONT_BOTHER_INC_CALL_POPUP               =   "DontBotherIncomingCallPopUp";

    //endregion

    //region Shared pref keys under SETTINGS
    public static final String WHO_CAN_MC_ME                    =   "WhoCanMCMe";
    public static final String BLOCK_LIST                       =   "BlockList";
    public static final String SAVE_MEDIA_OPTION                =   "SaveMediaOption";
    public static final String DOWNLOAD_ONLY_ON_WIFI            =   "DownloadOnlyOnWifi";
    //endregion

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

    public static Double getDouble(Context context, String prefsName, String key) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        return Double.longBitsToDouble(prefs.getLong(key, 0));
    }
    //endregion

    //region Setters
    public static void setDouble(Context context, String prefsName, String key, double value) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().putLong(key, Double.doubleToRawLongBits(value)).apply();
    }

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

    //region Removers

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

    /**
     * Removes all keys from shared preferences
     * @param context The application context
     * @param prefsName The shared preference to remove all keys from
     */
    public static void remove(Context context, String prefsName) {

        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    /**
     * Removes all keys from all shared preferences except app state
     * @param context
     */
    public static void removeAll(Context context) {

        for(String sharedPrefsName : allSharedPrefs) {
            remove(context, sharedPrefsName);
        }
    }
    //endregion
    //endregion

}
