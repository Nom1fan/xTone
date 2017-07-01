package com.ui.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.data.objects.Constants;
import com.mediacallz.app.R;
import com.services.ServerProxyService;
import com.utils.InitUtils;
import com.utils.UI_Utils;
import com.utils.UtilityFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.crashlytics.android.Crashlytics.log;


/**
 * Created by Mor on 14/03/2016.
 */
public class LoginWithTermsAndServiceActivity extends AppCompatActivity {

    public static final String SMS_CODE = "SMS_CODE";
    public static final String LOGIN_NUMBER = "LOGIN_NUMBER";

    private static final String TAG = LoginWithTermsAndServiceActivity.class.getSimpleName();
    private String loginNumber;
    private String smsVerificationCode;
    private  String[] initialPermissions = {Manifest.permission.READ_CONTACTS, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE,Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.PROCESS_OUTGOING_CALLS

            //, Manifest.permission.WRITE_SETTINGS, Manifest.permission.MODIFY_PHONE_STATE,Manifest.permission.CHANGE_CONFIGURATION

    };
    private Map<String,Boolean> permissionMap = new HashMap<>();
    private InitUtils initUtils = UtilityFactory.instance().getUtility(InitUtils.class);



    //region Activity methods (onCreate(), onPause(), ...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log(Log.INFO,TAG, "onCreate()");

        initializeTermsOfserviceUI();
        Intent intent = getIntent();
        smsVerificationCode = intent.getStringExtra(SMS_CODE);
        loginNumber = intent.getStringExtra(LOGIN_NUMBER);
    }

    @Override
    protected void onPause() {
        super.onPause();
        log(Log.INFO,TAG, "onPause()");
    }

    @Override
    protected void onResume() {
        super.onResume();

        log(Log.INFO,TAG, "OnResume()");
    }
    //endregion

    //region UI methods
    private void initializeTermsOfserviceUI() {

        setContentView(R.layout.login_termsofservice);

        prepareCancelButton();

        prepareLoginButton();

        prepareReadMorebutton();

        prepareArrowView();
    }

    private void prepareCancelButton() {
        Button cancel = (Button) findViewById(R.id.cancel_login);
        if (cancel != null) {
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    setResult(Activity.RESULT_CANCELED);
                    finish();
                }
            });
        }
    }

    private void prepareLoginButton() {
        Button login = (Button) findViewById(R.id.login_continue);
        if (login != null) {
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                        for (String permission: initialPermissions)
                        {
                            if (ContextCompat.checkSelfPermission(LoginWithTermsAndServiceActivity.this, permission) == -1)
                            {
                                ActivityCompat.requestPermissions(LoginWithTermsAndServiceActivity.this, initialPermissions, Constants.MY_PERMISSIONS_INITAL_PERMISSION);
                                return;
                            }
                        }

                        registering();

                    }
                    else
                        registering();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_INITAL_PERMISSION: {
                boolean isPermissionsApproved= true;

                for (String permission: initialPermissions)
                {
                    if (ContextCompat.checkSelfPermission(LoginWithTermsAndServiceActivity.this, permission) == -1)
                    {
                        permissionMap.put(permission,false);
                        isPermissionsApproved=false;
                    }else
                        permissionMap.put(permission,true);
                }

                if (isPermissionsApproved)
                    registering();
                else
                    showRationale();


                break;
            }
        }
    }

    private void showRationale() {
        boolean neverAskButtonCheckBoxNotTicked = true;

        for (Map.Entry<String, Boolean> entry : permissionMap.entrySet()) {
            if (!entry.getValue())
                if(!shouldShowRequestPermissionRationale(entry.getKey()))
                    neverAskButtonCheckBoxNotTicked = false;
        }

            if (!neverAskButtonCheckBoxNotTicked) {
                // user also CHECKED "never ask again"
                // you can either enable some fall back,
                // disable features of your app
                // or open another dialog explaining
                // again the permission and directing to
                // the app setting
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                TextView content = new TextView(this);
                content.setText(R.string.permission_a_must);

                builder.setTitle(R.string.permission_denied)
                        .setView(content)
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                dialog.cancel();

                            }
                        })
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivityForResult(intent, 1985);
                            }
                        });
                builder.create().show();
            } else {
                // user did NOT check "never ask again"
                // this is a good place to explain the user
                // why you need the permission and ask if he wants
                // to accept it (the rationale)
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                TextView content = new TextView(this);
                content.setText(R.string.permission_a_must);

                builder.setTitle(R.string.permission_denied)
                        .setView(content)
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                dialog.cancel();

                            }
                        })
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ActivityCompat.requestPermissions(LoginWithTermsAndServiceActivity.this, initialPermissions, Constants.MY_PERMISSIONS_INITAL_PERMISSION);
                            }
                        });
                builder.create().show();

            }

    }

    private void registering() {

        InitializeAppConfigurations();

        Constants.MY_ID(getApplicationContext(), loginNumber);
        ServerProxyService.register(getApplicationContext(), Integer.parseInt(smsVerificationCode));

        setResult(Activity.RESULT_OK);
        finish();
    }

    private void InitializeAppConfigurations() {
        Context context = getApplicationContext();

        //make sure TitleBar Menu Appears in all devices (don't matter if they have HARD menu button or not)
        UI_Utils.makeActionOverflowMenuShown(context);

        // This will prevent Android's media scanner from reading your media files and including them in apps like Gallery or Music.
        initUtils.hideMediaFromGalleryScanner();

        //Initialize Default Settings Values
        initUtils.initializeSettingsDefaultValues(context);

        //Populate SharedprefMEdia in case it's not the first time the app is installed, and you have saved media in the MediaCallz Outgoing/Incoming
        initUtils.populateSavedMcFromDiskToSharedPrefs(context);

        initUtils.saveAndroidVersion(context);

        initUtils.initImageLoader(context);
    }

    private void prepareReadMorebutton() {
        TextView read_more = (TextView) findViewById(R.id.login_read_more);
        if (read_more != null) {
            read_more.setEnabled(true);
            read_more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String url = Constants.TERMS_AND_PRIVACY_URL;
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }
            });
        }
    }

    private void prepareArrowView() {
        ImageView arrow = (ImageView) findViewById(R.id.arrow);

        if(arrow != null) {
            if (!Locale.getDefault().getLanguage().contains("en")) {
                arrow.setScaleX(-1f);
            }

            arrow.bringToFront();
            arrow.setVisibility(View.VISIBLE);
        }
    }

    //endregion
}

