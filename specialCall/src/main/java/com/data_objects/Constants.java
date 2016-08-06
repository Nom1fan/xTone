package com.data_objects;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import com.utils.SharedPrefUtils;

import java.io.File;

import DataObjects.SharedConstants;

import static com.crashlytics.android.Crashlytics.log;

public class Constants {

    private static final String TAG = Constants.class.getSimpleName();

    // Initialized using InitUtils in MediaCallzApp class
    public static double APP_VERSION(Context context) {
        Double appVersion;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            appVersion = Double.valueOf(packageInfo.versionName);
        } catch(PackageManager.NameNotFoundException e) {
            log(Log.ERROR,TAG, "Failed to retrieve app version. Setting default app version for emergency!");
            appVersion = 1.10;
        }
        return appVersion;
    }

    // Constants for Batch
    public static final String GCM_SENDER_ID = "817954308887";

    // Constants for Website
    public static final String TERMS_AND_PRIVACY_URL = "http://www.mediacallz.com/terms-of-service---privacy-policy.html";
    public static final String MEDIACALLZ_CONTENT_STORE_URL = "http://" + SharedConstants.STROAGE_SERVER_HOST + "/ContentStore/";

    private static final String INCOMING_FOLDER_NAME = SharedConstants.APP_NAME + "_Incoming";
    private static final String OUTGOING_FOLDER_NAME = SharedConstants.APP_NAME + "_Outgoing";
    private static final String COMP_FOLDER_NAME = SharedConstants.APP_NAME + "_Compressed";
    private static final String HISTORY_FOLDER_NAME =  SharedConstants.APP_NAME + "_History";
    public static final String ROOT_FOLDER = setRootFolder();
    public static final String INCOMING_FOLDER = getIncomingFolder();
    public static final String OUTGOING_FOLDER = getOutgoingFolder();
    public static final String COMPRESSED_FOLDER = getFolderForCompression();
    public static final String HISTORY_FOLDER = getHistoryFolderForCompression();

    public static String MY_ID(Context context) { return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER); }
    public static void MY_ID(Context context, String number) { SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER, number); }

    public static String MY_BATCH_TOKEN(Context context) { return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_BATCH_TOKEN); }
    public static void MY_BATCH_TOKEN(Context context, String token) { SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_BATCH_TOKEN, token); }

    // Android version upon register, and then used to sync with server after every upgrade
    public static String MY_ANDROID_VERSION(Context context) { return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.ANDROID_VERSION); }
    public static void MY_ANDROID_VERSION(Context context, String androidVersion) { SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.ANDROID_VERSION, androidVersion); }

    private static String getIncomingFolder() {

        String path = ROOT_FOLDER + INCOMING_FOLDER_NAME + "/";
        File incomingFolder = new File(path);
        incomingFolder.mkdirs();
        SharedConstants.INCOMING_FOLDER = path;
        return path;
    }

    private static String getOutgoingFolder() {

        String path = ROOT_FOLDER + OUTGOING_FOLDER_NAME + "/";
        File outgoingFolder = new File(path);
        outgoingFolder.mkdirs();
        SharedConstants.OUTGOING_FOLDER = path;
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

    private static String setRootFolder() {

        String path = Environment.getExternalStorageDirectory() + "/" + SharedConstants.APP_NAME + "/";
        File rootFolder = new File(path);
        rootFolder.mkdirs();
        SharedConstants.ROOT_FOLDER = path;
        return path;
    }

}
