package com.ui.activities;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.mediacallz.app.R;
import com.ui.dialogs.DeleteAccountDialog;
import com.utils.SharedPrefUtils;

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

        CheckBoxPreference ask_before_show_checkbox = (CheckBoxPreference) findPreference("ask_before_show");

        ask_before_show_checkbox.setChecked(SharedPrefUtils.getBoolean(getActivity().getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.ASK_BEFORE_MEDIA_SHOW));
        ask_before_show_checkbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean checked = Boolean.valueOf(newValue.toString());

                SharedPrefUtils.setBoolean(getActivity().getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.ASK_BEFORE_MEDIA_SHOW, checked);

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