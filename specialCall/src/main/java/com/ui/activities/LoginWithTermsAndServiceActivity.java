package com.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.data.objects.Constants;
import com.mediacallz.app.R;
import com.services.ServerProxyService;

import java.util.Locale;

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
                    Constants.MY_ID(getApplicationContext(), loginNumber);
                    ServerProxyService.register(getApplicationContext(), Integer.parseInt(smsVerificationCode));

                    setResult(Activity.RESULT_OK);
                    finish();
                }
            });
        }
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

