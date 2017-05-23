package com.utils;

import android.content.Context;

import com.data.objects.PermissionBlockListLevel;
import com.data.objects.SaveMediaOption;

import static com.data.objects.PermissionBlockListLevel.EMPTY;
import static com.data.objects.PermissionBlockListLevel.valueOf;

/**
 * Created by Mor on 22/05/2017.
 */

public abstract class SettingsUtils {

    static final String SETTINGS = "SETTINGS";
    static final String WHO_CAN_MC_ME = "WHO_CAN_MC_ME";

    public static void setWhoCanMCMe(Context context, PermissionBlockListLevel permissionBlockListLevel) {
        SharedPrefUtils.setString(context, SETTINGS, WHO_CAN_MC_ME, permissionBlockListLevel.getValue());
    }

    public static PermissionBlockListLevel getWhoCanMCMe(Context context) {
        String whoCanMCMe = SharedPrefUtils.getString(context, SETTINGS, WHO_CAN_MC_ME);
        return whoCanMCMe == null || whoCanMCMe.isEmpty() ? EMPTY : valueOf(whoCanMCMe);
    }

    public static boolean isDownloadOnlyOnWifi(Context context) {
        return SharedPrefUtils.getBoolean(context, SETTINGS, SharedPrefUtils.DOWNLOAD_ONLY_ON_WIFI);
    }

    public static void setDownloadOnlyOnWifi(Context context, boolean b) {
        SharedPrefUtils.setBoolean(context, SETTINGS, SharedPrefUtils.DOWNLOAD_ONLY_ON_WIFI, b);
    }

    public static void setAskBeforeShowingMedia(Context context, boolean b) {
        SharedPrefUtils.setBoolean(context, SETTINGS, SharedPrefUtils.ASK_BEFORE_SHOWING_MEDIA, b);
    }

    public static boolean getAskBeforeShowingMedia(Context context) {
        return SharedPrefUtils.getBoolean(context, SETTINGS, SharedPrefUtils.ASK_BEFORE_SHOWING_MEDIA);
    }

    public static void setSaveMediaOption(Context context, SaveMediaOption saveMediaOption) {
        SharedPrefUtils.setInt(context, SETTINGS, SharedPrefUtils.SAVE_MEDIA_OPTION, saveMediaOption.getValue());
    }

    public static SaveMediaOption getSaveMediaOption(Context context) {
        return SaveMediaOption.fromValue(SharedPrefUtils.getInt(context, SETTINGS, SharedPrefUtils.SAVE_MEDIA_OPTION, 0));
    }

    public static void setStrictRingingCapabilitiesDevice(Context context, boolean b) {
        SharedPrefUtils.setBoolean(context, SETTINGS, SharedPrefUtils.STRICT_RINGING_CAPABILITIES_DEVICES, b);
    }

    public static boolean isStrictRingingCapabilitiesDevice(Context context) {
        return SharedPrefUtils.getBoolean(context, SETTINGS, SharedPrefUtils.STRICT_RINGING_CAPABILITIES_DEVICES);
    }
}
