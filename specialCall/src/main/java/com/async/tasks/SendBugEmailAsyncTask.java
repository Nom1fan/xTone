package com.async.tasks;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.data.objects.Constants;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.utils.SpecialDevicesUtils;

import java.io.File;
import java.io.IOException;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 03/09/2016.
 */
public class SendBugEmailAsyncTask extends AsyncTask<Void,Void,String> {

    private static final String TAG = SendBugEmailAsyncTask.class.getSimpleName();
    private static final String to[] = {
            "ronyahae@gmail.com",
            "mormerhav@gmail.com"
    };
    private Activity activity;


    public SendBugEmailAsyncTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected String doInBackground(Void... params) {
        AdvertisingIdClient.Info idInfo = null;
        String advertId = null;
        try {
            try {
                idInfo = AdvertisingIdClient.getAdvertisingIdInfo(activity);
            } catch (GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException | IOException e) {
                e.printStackTrace();
            }

            try {
                advertId = idInfo != null ? idInfo.getId() : null;
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return advertId;
    }

    @Override
    protected void onPostExecute(String advertId) {

        sendBugReport(advertId, activity);
        log(Log.INFO, TAG, "Google Ad ID: " + advertId);

    }

    private void sendBugReport(String GAID, Activity activity) {

        // save logcat in file
        File outputFile = new File(Constants.ROOT_FOLDER,
                "logcat.txt");
        try {
            Runtime.getRuntime().exec(
                    "logcat -f " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //send file using email
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        // Set type to "email"
        emailIntent.setType("vnd.android.cursor.dir/email");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        // the attachment
        Uri uri = Uri.fromFile(outputFile);
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);

        if (activity != null) {
            emailIntent.putExtra(Intent.EXTRA_TEXT,
                    "\n MY_ID: " + Constants.MY_ID(activity)
                            + "\n BATCH_INSTALLATION_ID: " + Constants.MY_BATCH_TOKEN(activity)
                            + "\n GOOGLE_AD_ID: " + GAID
                            + "\n Device Model: " + SpecialDevicesUtils.getDeviceName()
                            + "\n MY_ANDROID_VERSION: " + Constants.MY_ANDROID_VERSION(activity)
                            + "\n MediaCallz App Version: " + Constants.APP_VERSION()
            );

            // the mail subject
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "MediaCallz_Logs Received from: " + Constants.MY_ID(activity));
        }
        if (activity != null) {
            activity.startActivity(Intent.createChooser(emailIntent, "Send email..."));
        }

    }
}
