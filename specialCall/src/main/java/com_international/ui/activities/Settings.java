package com_international.ui.activities;

import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com_international.enums.SaveMediaOption;
import com_international.mediacallz.app.R;
import com_international.ui.dialogs.DeleteAccountDialog;
import com_international.utils.SettingsUtils;

import static com.crashlytics.android.Crashlytics.log;

public class Settings extends PreferenceFragment {

    private static final String TAG = Settings.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getActivity().getApplicationContext();
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        CheckBoxPreference wifi_download_only = (CheckBoxPreference) findPreference("wifi_download_only");

        wifi_download_only.setChecked(SettingsUtils.isDownloadOnlyOnWifi(context));
        wifi_download_only.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean checked = Boolean.valueOf(newValue.toString());
                SettingsUtils.setDownloadOnlyOnWifi(context, checked);
                return true;
            }
        });

        ListPreference save_media_listPreference = (ListPreference) findPreference("save_media");
        SaveMediaOption saveMediaOption = SettingsUtils.getSaveMediaOption(context);
        save_media_listPreference.setValueIndex(saveMediaOption.getValue());
        save_media_listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int checked = Integer.parseInt(newValue.toString());
                SettingsUtils.setSaveMediaOption(context, SaveMediaOption.fromValue(checked));
                log(Log.INFO,TAG,"save Media Option:" + checked);
                return true;
            }
        });

        CheckBoxPreference ringing_override_checkbox = (CheckBoxPreference) findPreference("ringing_override_checkbox");

        boolean strictRingingCapabilitiesDevice = SettingsUtils.isStrictRingingCapabilitiesDevice(context);
        ringing_override_checkbox.setChecked(strictRingingCapabilitiesDevice);
        ringing_override_checkbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean checked = Boolean.valueOf(newValue.toString());
                SettingsUtils.setStrictRingingCapabilitiesDevice(context, checked);
                return true;
            }
        });

        CheckBoxPreference ask_before_show_checkbox = (CheckBoxPreference) findPreference("ask_before_show");

        ask_before_show_checkbox.setChecked(SettingsUtils.getAskBeforeShowingMedia(context));
        ask_before_show_checkbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean checked = Boolean.valueOf(newValue.toString());
                SettingsUtils.setAskBeforeShowingMedia(context, checked);
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