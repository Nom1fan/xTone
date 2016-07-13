package com.ui.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.utils.SharedPrefUtils;

public class AboutAndHelp extends PreferenceFragment {

    private static final String TAG = AboutAndHelp.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.about_help);

        //region Help
        Preference howToMc = findPreference("how_to_mc");
        howToMc.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {


                SharedPrefUtils.setString(getActivity().getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER, "");
                SharedPrefUtils.setString(getActivity().getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME, "");

                SharedPrefUtils.setBoolean(getActivity().getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW, false);
                SharedPrefUtils.setBoolean(getActivity().getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.SELECT_MEDIA_VIEW, false);
                SharedPrefUtils.setBoolean(getActivity().getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.UPLOAD_BEFORE_CALL_VIEW, false);
                SharedPrefUtils.setBoolean(getActivity().getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.DONT_SHOW_AGAIN_TIP, false);
                SharedPrefUtils.setBoolean(getActivity().getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.DONT_SHOW_AGAIN_CLEAR_DIALOG, false);
                SharedPrefUtils.setBoolean(getActivity().getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.DONT_SHOW_AGAIN_UPLOAD_DIALOG, false);

                final Intent i = new Intent(getActivity() , MainActivity.class);
                startActivity(i);
                getActivity().finish();


                return true;
            }
        });
        //endregion

        //region shareAndRate
        Preference shareUs = findPreference("share_us");
        shareUs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "MediaCallz (Open it in Google Play Store to Download the Application)");

                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, getResources().getString(R.string.invite));
                startActivity(Intent.createChooser(sharingIntent, "Share via"));


                return true;
            }
        });

        Preference rateUs = findPreference("rate_us");
        rateUs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Uri uri = Uri.parse("market://details?id=" + getActivity().getApplicationContext().getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                // To count with Play market backstack, After pressing back button,
                // to taken back to our application, we need to add following flags to intent.
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + getActivity().getApplicationContext().getPackageName())));
                }

                return true;
            }
        });
        //endregion

        //region About

        Preference terms = findPreference("terms");
        terms.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                String url = Constants.TERMS_AND_PRIVACY_URL;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);

                return true;
            }
        });


        Preference version = findPreference("version");
        String appVersion = String.valueOf(Constants.APP_VERSION(getActivity()));
        version.setSummary(appVersion);
        //endregion

        Preference mailUs = findPreference("mail");
        mailUs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_EMAIL, "mediacallzapp@gmail.com");
                /*intent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
                intent.putExtra(Intent.EXTRA_TEXT, "I'm email body.");*/

                startActivity(Intent.createChooser(intent, "Send Email"));
                return true;
            }
        });
    }
}