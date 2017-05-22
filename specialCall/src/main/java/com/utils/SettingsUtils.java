package com.utils;

import android.content.Context;

import com.data.objects.PermissionBlockListLevel;
import com.data.objects.PermissionBlockListLevelEnum;

/**
 * Created by Mor on 22/05/2017.
 */

public abstract class SettingsUtils {

    public static void setWhoCanMCMe(Context context, PermissionBlockListLevelEnum  permissionBlockListLevel) {
        SharedPrefUtils.setString(context, SharedPrefUtils.SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME, permissionBlockListLevel.getValue());
    }

    public static PermissionBlockListLevelEnum getWhoCanMCMe(Context context) {
        String whoCanMCMe = SharedPrefUtils.getString(context, SharedPrefUtils.SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME);
        return PermissionBlockListLevelEnum.valueOf(whoCanMCMe);
    }


}
