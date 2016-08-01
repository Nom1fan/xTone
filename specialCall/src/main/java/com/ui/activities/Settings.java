package com.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.ui.dialogs.DeleteAccountDialog;
import com.utils.SharedPrefUtils;
import com.utils.SpecialDevicesUtils;

import java.io.File;
import java.io.IOException;

import DataObjects.SharedConstants;

import static com.crashlytics.android.Crashlytics.log;

public class Settings extends PreferenceFragment {

    private static final String TAG = Settings.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        CheckBoxPreference wifi_download_only = (CheckBoxPreference) findPreference("wifi_download_only");

        wifi_download_only.setChecked(SharedPrefUtils.getBoolean(getActivity().getApplicationContext(), SharedPrefUtils.SETTINGS, SharedPrefUtils.DOWNLOAD_ONLY_ON_WIFI));
        wifi_download_only.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean checked = Boolean.valueOf(newValue.toString());

                SharedPrefUtils.setBoolean(getActivity().getApplicationContext(), SharedPrefUtils.SETTINGS, SharedPrefUtils.DOWNLOAD_ONLY_ON_WIFI, checked);

                return true;
            }
        });


        // SAVE MEDIA : 0 - always , 1 - contacts only , 2 - never save
        ListPreference save_media_listPreference = (ListPreference) findPreference("save_media");
        save_media_listPreference.setValueIndex(SharedPrefUtils.getInt(getActivity().getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.SAVE_MEDIA_OPTION, 0));
        save_media_listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                int checked = Integer.parseInt(newValue.toString());

                SharedPrefUtils.setInt(getActivity().getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.SAVE_MEDIA_OPTION, checked);
                log(Log.INFO,TAG,"save Media Option: " + String.valueOf(checked) );


                return true;
            }
        });


        CheckBoxPreference ringing_override_checkbox = (CheckBoxPreference) findPreference("ringing_override_checkbox");

        ringing_override_checkbox.setChecked(SharedPrefUtils.getBoolean(getActivity().getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.STRICT_RINGING_CAPABILITIES_DEVICES));
        ringing_override_checkbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean checked = Boolean.valueOf(newValue.toString());

                SharedPrefUtils.setBoolean(getActivity().getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.STRICT_RINGING_CAPABILITIES_DEVICES, checked);

                return true;
            }
        });



        Preference sendLogs = findPreference("Send Logs");
        sendLogs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {


                Log.i(TAG,"SEND LOGS BUTTON PRESSED");
                // save logcat in file
                File outputFile = new File(SharedConstants.ROOT_FOLDER,
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
                String to[] = {"ronyahae@gmail.com" , "mormerhav@gmail.com"};
                emailIntent .putExtra(Intent.EXTRA_EMAIL, to);
                // the attachment
                Uri uri = Uri.fromFile(outputFile);
                emailIntent .putExtra(Intent.EXTRA_STREAM, uri);

                emailIntent.putExtra(Intent.EXTRA_TEXT,
                        "\n MY_ID: " + Constants.MY_ID(getContext())
                    +   "\n MY_BATCH_TOKEN: " + Constants.MY_BATCH_TOKEN(getContext())
                    +   "\n Device Model: " + SpecialDevicesUtils.getDeviceName()
                    +   "\n MY_ANDROID_VERSION: " + Constants.MY_ANDROID_VERSION(getContext())
                    +   "\n MediaCallz App Version: " + Constants.APP_VERSION(getContext())
                );

                // the mail subject
                emailIntent .putExtra(Intent.EXTRA_SUBJECT, "MediaCallz_Logs Received from: " + Constants.MY_ID(getContext()));
                startActivity(Intent.createChooser(emailIntent , "Send email..."));

                return true;
            }
        });

        Preference button = findPreference("Delete Account");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                DeleteAccountDialog deleteAccountDialog = new DeleteAccountDialog();
                deleteAccountDialog.show(getFragmentManager(), TAG);
                return true;
            }
        });
    }
}