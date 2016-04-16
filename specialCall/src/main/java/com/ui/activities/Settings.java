package com.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.mediacallz.app.R;
import com.services.IncomingService;
import com.ui.dialogs.DeleteAccountDialog;
import com.utils.SharedPrefUtils;

public class Settings extends PreferenceFragment {

    private static final String TAG = Settings.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        CheckBoxPreference wifi_download_only = (CheckBoxPreference) findPreference("wifi_download_only");

        wifi_download_only.setChecked(SharedPrefUtils.getBoolean(getActivity().getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DOWNLOAD_ONLY_ON_WIFI));
        wifi_download_only.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean checked = Boolean.valueOf(newValue.toString());

                SharedPrefUtils.setBoolean(getActivity().getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DOWNLOAD_ONLY_ON_WIFI, checked);

                return true;
            }
        });


        CheckBoxPreference save_media = (CheckBoxPreference) findPreference("save_media");

        save_media.setChecked(SharedPrefUtils.getBoolean(getActivity().getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.ALWAYS_SAVE_MEDIA));
        save_media.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean checked = Boolean.valueOf(newValue.toString());

                SharedPrefUtils.setBoolean(getActivity().getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.ALWAYS_SAVE_MEDIA, checked);

                return true;
            }
        });


        CheckBoxPreference foreground_checkbox = (CheckBoxPreference) findPreference("foreground_checkbox");

        foreground_checkbox.setChecked(SharedPrefUtils.getBoolean(getActivity().getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.STRICT_MEMORY_MANAGER_DEVICES));
        foreground_checkbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean checked = Boolean.valueOf(newValue.toString());

                SharedPrefUtils.setBoolean(getActivity().getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.STRICT_MEMORY_MANAGER_DEVICES, checked);

                if (!checked) {
                    Intent incomingServiceIntent = new Intent(getActivity().getApplicationContext(), IncomingService.class);
                    incomingServiceIntent.setAction(IncomingService.ACTION_STOP_FOREGROUND);
                    getActivity().startService(incomingServiceIntent);
                } else {
                    Intent incomingServiceIntent = new Intent(getActivity().getApplicationContext(), IncomingService.class);
                    incomingServiceIntent.setAction(IncomingService.ACTION_START_FOREGROUND);
                    getActivity().startService(incomingServiceIntent);
                }

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