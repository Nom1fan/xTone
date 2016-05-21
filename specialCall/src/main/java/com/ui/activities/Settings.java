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
                Log.i(TAG,"save Media Option: " + String.valueOf(checked) );


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