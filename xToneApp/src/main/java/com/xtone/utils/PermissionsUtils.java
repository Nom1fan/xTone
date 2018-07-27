package com.xtone.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AlertDialog;

import com.xtone.app.R;
import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;

import static android.Manifest.permission.READ_CALL_LOG;
import static android.Manifest.permission.READ_PHONE_STATE;

public class PermissionsUtils implements Utility {

    private static final String TAG = PermissionsUtils.class.getSimpleName();

    private Logger log = LoggerFactory.getLogger();

    public static final String[] PERMISSIONS = { READ_PHONE_STATE, READ_CALL_LOG };
    public static final int PERMISSION_REQUEST = 100;

    public boolean checkPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return canReadPhoneState(activity);
        }
        return true;
    }

    public boolean isPermissionGranted(int[] grantResults) {
        return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }


    public boolean hasPermission(Context activity, String permission) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (PackageManager.PERMISSION_GRANTED == activity.checkSelfPermission(permission));
    }

    public void alertAndFinish(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.app_name).setMessage(activity.getString(R.string.permissions_denial));

        // Add the buttons.
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public Boolean canReadPhoneState(Activity activity) {
        return (hasPermission(activity, READ_PHONE_STATE));
    }
}
