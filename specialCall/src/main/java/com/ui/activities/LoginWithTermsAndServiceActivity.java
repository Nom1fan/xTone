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

import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.services.LogicServerProxyService;


/**
 * Created by Mor on 14/03/2016.
 */
public class LoginWithTermsAndServiceActivity extends AppCompatActivity {

    public static final String SmsCode = "SMS_CODE";
    public static final String LoginNumber = "LOGIN_NUMBER";

    private static final String TAG = LoginWithTermsAndServiceActivity.class.getSimpleName();
    private String _loginNumber;
    private String _smsVerificationCode;

    //region Activity methods (onCreate(), onPause(), ...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        initializeTermsOfserviceUI();
        Intent intent = getIntent();
        _smsVerificationCode = intent.getStringExtra(SmsCode);
        _loginNumber = intent.getStringExtra(LoginNumber);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "OnResume()");
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
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
    }

    private void prepareLoginButton() {

        Button login = (Button) findViewById(R.id.login_continue);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Constants.MY_ID(getApplicationContext(), _loginNumber);

                Intent i = new Intent(getApplicationContext(), LogicServerProxyService.class);
                i.setAction(LogicServerProxyService.ACTION_REGISTER);
                i.putExtra(LogicServerProxyService.SMS_CODE, Integer.parseInt(_smsVerificationCode));
                getApplicationContext().startService(i);

                setResult(Activity.RESULT_OK);
                finish();
            }
        });
    }

    private void prepareReadMorebutton() {

        TextView read_more = (TextView) findViewById(R.id.login_read_more);
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

    private void prepareArrowView() {

        ImageView arrow = (ImageView) findViewById(R.id.arrow);
        arrow.bringToFront();
        arrow.setVisibility(View.VISIBLE);
    }

    //endregion
}

