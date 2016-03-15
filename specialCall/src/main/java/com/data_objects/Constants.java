package com.data_objects;

import android.content.Context;
import android.os.Environment;

import com.utils.SharedPrefUtils;

import java.io.File;

import DataObjects.SharedConstants;

public class Constants {

    // Constants for Parse
    public static final String APPLICATION_ID = "7CL97UlX4EtpMyRJYshNlIQ3T12EEZ0OaZWxZjvR";
    public static final String CLIENT_KEY = "7Elu6v6XVyQRzxIqnlyIG9YGyzXuh65hD42ZUqZa";

    // Constants for Batch
    public static final String GCM_SENDER_ID = "908225653874";

    private static final String INCOMING_FOLDER_NAME = "Incoming_" + SharedConstants.APP_NAME;
    private static final String OUTGOING_FOLDER_NAME = "Outgoing_" + SharedConstants.APP_NAME;
    private static final String TEMP_COMP_FOLDER_NAME = "Compressed_" + SharedConstants.APP_NAME;
    private static final String HISTORY_FOLDER_NAME =  SharedConstants.APP_NAME +"_History";
    private static final String TEMP_RECORDING_FOLDER_NAME = "TempRecording";
    public static final String ROOT_FOLDER = setRootFolder();
    public static final String INCOMING_FOLDER = getIncomingFolder();
    public static final String OUTGOING_FOLDER = getOutgoingFolder();
    public static final String TEMP_COMPRESSED_FOLDER = getTempFolderForCompression();
    public static final String HISTORY_FOLDER = ROOT_FOLDER + HISTORY_FOLDER_NAME + "/";
    public static final String TEMP_RECORDING_FOLDER = TEMP_COMPRESSED_FOLDER + TEMP_RECORDING_FOLDER_NAME + "/";

    public static String MY_ID(Context context) { return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER); }

    public static String MY_BATCH_TOKEN(Context context) { return SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_BATCH_TOKEN); }
    public static void MY_BATCH_TOKEN(Context context, String token) { SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_BATCH_TOKEN, token); }

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

    private static String getTempFolderForCompression() {

        String path = ROOT_FOLDER + TEMP_COMP_FOLDER_NAME + "/";
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
