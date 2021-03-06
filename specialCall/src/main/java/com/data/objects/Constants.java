package com.data.objects;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.mediacallz.app.BuildConfig;
import com.utils.SharedPrefUtils;

import java.io.File;

import static com.crashlytics.android.Crashlytics.log;

public abstract class Constants {

    private static final String TAG = Constants.class.getSimpleName();

    //region App
    public static final String APP_NAME = "MediaCallz";

    public static double APP_VERSION() {
        return Double.valueOf(BuildConfig.VERSION_NAME);
    }

    public static String MY_ID(Context context) { return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER); }
    public static void MY_ID(Context context, String number) { SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER, number); }

    // Android version upon register, and then used to sync with server after every upgrade
    public static String MY_ANDROID_VERSION(Context context) { return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.ANDROID_VERSION); }
    public static void MY_ANDROID_VERSION(Context context, String androidVersion) { SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.ANDROID_VERSION, androidVersion); }
    //endregion

    //region Device Hardware
    private static String MY_DEVICE_MODEL;

    public static String MY_DEVICE_MODEL() {
        if(MY_DEVICE_MODEL == null) {
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            if (model.startsWith(manufacturer)) {
                return model.toLowerCase();
            }
            log(Log.INFO, TAG, "Manufacturer device name: " + manufacturer + " " + model);
            MY_DEVICE_MODEL = (manufacturer + " " + model).toLowerCase();
        }
        return MY_DEVICE_MODEL;
    }
    //endregion

    //region Connection to server
//    public static final String SERVER_HOST = "staging.mediacallz.com";
//    public static final int SERVER_PORT = 8080;
    public static final String SERVER_HOST = "server.mediacallz.com";
    public static final int SERVER_PORT = 8080;
    //endregion

    //region Push
    public static String MY_FIREBASE_TOKEN(Context context) { return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_FIREBASE_TOKEN); }
    public static void MY_FIREBASE_TOKEN(Context context, String token) { SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_FIREBASE_TOKEN, token); }
    //endregion

    //region Website
    public static final String TERMS_AND_PRIVACY_URL = "http://www.mediacallz.com/page/%D7%AA%D7%A0%D7%90%D7%99%D7%9D%20%D7%95%D7%A4%D7%A8%D7%98%D7%99%D7%95%D7%AA";
    //endregion

    //region Folder names
    private static final String INCOMING_FOLDER_NAME = "Incoming_" + APP_NAME;
    private static final String OUTGOING_FOLDER_NAME = "Outgoing_" + APP_NAME;
    private static final String COMP_FOLDER_NAME = APP_NAME + "_Compressed";
    private static final String HISTORY_FOLDER_NAME = APP_NAME +"_History";
    private static final String AUDIO_FOLDER_NAME = "AudioHistory";
    //endregion

    //region Folder paths
    public static final String ROOT_FOLDER = getRootFolder();
    public static final String INCOMING_FOLDER = getIncomingFolder();
    public static final String OUTGOING_FOLDER = getOutgoingFolder();
    public static final String AUDIO_HISTORY_FOLDER = getAudioHistoryFolder();
    public static final String COMPRESSED_FOLDER = getFolderForCompression();
    public static final String HISTORY_FOLDER = getHistoryFolderForCompression();
    //endregion

    //region Content Store
    public static final String STORE_URL = "http://server.mediacallz.com/ContentStore/files/";
    public static final String IMAGE_LIB_URL = STORE_URL + "Image/";
    public static final String GIF_LIB_URL = STORE_URL + "Gif/";
    public static final String AUDIO_LIB_URL = STORE_URL + "Audio/";
    public static final String AUDIO_THUMBS_URL = STORE_URL + "Audio/Thumbnails/";
    public static final String VIDEO_LIB_URL = STORE_URL + "Video/";
    public static final String VIDEO_THUMBS_URL = STORE_URL + "Video/Thumbnails/";
    //endregion

    //region Getters
    private static String getIncomingFolder() {

        String path = ROOT_FOLDER + INCOMING_FOLDER_NAME + "/";
        File incomingFolder = new File(path);
        incomingFolder.mkdirs();
        return path;
    }

    private static String getOutgoingFolder() {

        String path = ROOT_FOLDER + OUTGOING_FOLDER_NAME + "/";
        File outgoingFolder = new File(path);
        outgoingFolder.mkdirs();
        return path;
    }

    private static String getAudioHistoryFolder() {

        String path = ROOT_FOLDER + HISTORY_FOLDER_NAME +  "/" + AUDIO_FOLDER_NAME + "/" ;
        File AudioHistoryFolder = new File(path);
        AudioHistoryFolder.mkdirs();
        return path;
    }

    private static String getFolderForCompression() {

        String path = ROOT_FOLDER + COMP_FOLDER_NAME + "/";
        File outgoingFolder = new File(path);
        outgoingFolder.mkdirs();
        return path;
    }

    private static String getHistoryFolderForCompression() {
        String path = ROOT_FOLDER + HISTORY_FOLDER_NAME + "/";
        File outgoingFolder = new File(path);
        outgoingFolder.mkdirs();
        return path;
    }

    private static String getRootFolder() {

        String path = Environment.getExternalStorageDirectory() + "/" + APP_NAME + "/";
        File rootFolder = new File(path);
        rootFolder.mkdirs();
        return path;
    }
    //endregion

}
