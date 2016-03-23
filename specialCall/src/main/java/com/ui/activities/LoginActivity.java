package com.ui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.app.AppStateManager;
import com.batch.android.Batch;
import com.data_objects.Constants;
import com.services.GetTokenIntentService;
import com.services.LogicServerProxyService;
import com.special.app.R;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.util.Locale;

import EventObjects.Event;
import EventObjects.EventReport;


/**
 * Created by Mor on 14/03/2016.
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private BroadcastReceiver _eventReceiver;
    private IntentFilter _eventIntentFilter = new IntentFilter(Event.EVENT_ACTION);

    //region Activity methods (onCreate(), onPause(), ...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        Locale current = getResources().getConfiguration().locale;

    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.i(TAG, "OnStart()");

        Batch.onStart(this);

        prepareEventReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");

        if (_eventReceiver != null) {
            try {
                unregisterReceiver(_eventReceiver);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
        saveInstanceState();

        UI_Utils.unbindDrawables(findViewById(R.id.loginScreen));
        System.gc();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "OnResume()");

        initializeLoginUI();
        restoreInstanceState();

        if(Constants.MY_BATCH_TOKEN(this).equals("")) {

            Intent i = new Intent(this, GetTokenIntentService.class);
            i.setAction(GetTokenIntentService.ACTION_GET_BATCH_TOKEN);
            startService(i);

            setInitTextView(getResources().getString(R.string.initializing));
            enableProgressBar();
        }
        else
        {
            disableInitTextView();
            disableProgressBar();
        }
    }
    //endregion

    //region UI methods
    private void initializeLoginUI() {

        setContentView(R.layout.loginuser);

        prepareLoginNumberEditText();
        prepareLoginButton();
        prepareGetSmsCodeButton();
        prepareSmsCodeVerificationEditText();

    }

    private void enableProgressBar() {

        ProgressBar initProgressBar = (ProgressBar)findViewById(R.id.initProgressBar);
        if(initProgressBar!=null)
            initProgressBar.setVisibility(ProgressBar.VISIBLE);
    }

    private void disableProgressBar() {

        ProgressBar initProgressBar = (ProgressBar)findViewById(R.id.initProgressBar);
        if(initProgressBar!=null)
            initProgressBar.setVisibility(ProgressBar.INVISIBLE);
    }

    private void prepareLoginNumberEditText() {

        EditText loginNumberET = (EditText) findViewById(R.id.LoginNumber);
        loginNumberET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (10 == s.length()) {

                    String token = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_BATCH_TOKEN);
                    if (token != null && !token.equals("")) {
                        findViewById(R.id.GetSMSCode).setEnabled(true);
                        findViewById(R.id.SMSCodeEditText).setEnabled(true);
                        findViewById(R.id.login_btn).setEnabled(true);  // REMOVE // NEED TO FIND A SMS GATEWAY FIRST
                    }
                } else {
                    findViewById(R.id.GetSMSCode).setEnabled(false);
                    findViewById(R.id.SMSCodeEditText).setEnabled(false);
                    findViewById(R.id.login_btn).setEnabled(false);   // REMOVE // // NEED TO FIND A SMS GATEWAY FIRST
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void prepareLoginButton() {

        Button loginBtn = (Button) findViewById(R.id.login_btn);
        loginBtn.setOnClickListener(this);
        loginBtn.setEnabled(false);
        loginBtn.setText(getResources().getString(R.string.login));
    }

    private void prepareGetSmsCodeButton() {

        Button GetSMSCode = (Button) findViewById(R.id.GetSMSCode);
        GetSMSCode.setEnabled(false);
        View.OnClickListener buttonListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                EditText loginNumber = (EditText) findViewById(R.id.LoginNumber);
//
//                //generate a 4 digit integer 1000 <10000
//                //_randomPIN = (int) (Math.random() * 9000) + 1000;
//                try {
//                    SmsManager smsManager = SmsManager.getDefault();
//                    smsManager.sendTextMessage(loginNumber.getText().toString(), null, "MediaCallz SmsVerificationCode: " + String.valueOf(_randomPIN), null, null);
//                    writeInfoSnackBar("Message Sent To: " + loginNumber.getText());
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }

            }
        };
        GetSMSCode.setOnClickListener(buttonListener);
    }

    private void prepareSmsCodeVerificationEditText() {

        EditText SmsCodeVerificationEditText = (EditText) findViewById(R.id.SMSCodeEditText);
        SmsCodeVerificationEditText.setEnabled(false);
        SmsCodeVerificationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (4 == s.length()) {

                    findViewById(R.id.login_btn).setEnabled(true);

                } else
                    findViewById(R.id.login_btn).setEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void disableInitTextView() {

        final TextView initTextView = (TextView) findViewById(R.id.initTextView);
        if(initTextView!=null) {
            initTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void setInitTextView(String str) {

        final TextView initTextView = (TextView) findViewById(R.id.initTextView);
        if(initTextView!=null) {
            initTextView.setVisibility(View.VISIBLE);
            initTextView.setText(str);
        }
    }

    private void enableLoginButton() {

        Button loginBtn = (Button) findViewById(R.id.login_btn);
        loginBtn.setEnabled(true);
    }

    private void enableLoginEditText() {

        EditText loginNumberEditText = ((EditText) findViewById(R.id.LoginNumber));
        loginNumberEditText.setEnabled(true);
    }

    private void disableSmsCodeEditText() {

        EditText smsCodeVerificationEditText = (EditText) findViewById(R.id.SMSCodeEditText);
        smsCodeVerificationEditText.setEnabled(false);
    }

    private void disableLoginEditText() {

        EditText loginNumberEditText = ((EditText) findViewById(R.id.LoginNumber));
        loginNumberEditText.setEnabled(false);
    }

    private void disableLoginButton() {

        Button loginBtn = (Button) findViewById(R.id.login_btn);
        loginBtn.setEnabled(false);
    }

    private void disableSmsCodeButton() {

        Button smsCodeBtn = (Button) findViewById(R.id.GetSMSCode);
        smsCodeBtn.setEnabled(false);

    }

    private void continueToMainActivity() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                final Intent i = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        }, 1500);
    }
    //endregion

    //region Assisting methods (onClick(), eventReceived(), ...)
    public void onClick(View v) {

        int id = v.getId();

        if (id == R.id.login_btn) {

            String myVerificationcode = ((EditText) findViewById(R.id.SMSCodeEditText)).getText().toString();
            //if (myVerificationcode.equals(String.valueOf(_randomPIN))){    // NEED TO FIND A SMS GATEWAY FIRST

            String loginNumber = ((EditText) findViewById(R.id.LoginNumber))
                    .getText().toString();

            SharedPrefUtils.setString(getApplicationContext(),
                    SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER, loginNumber);

            Intent i = new Intent(this, LogicServerProxyService.class);
            i.setAction(LogicServerProxyService.ACTION_REGISTER);
            startService(i);
        }
    }

    public void eventReceived(Event event) {

        final EventReport report = event.report();

        switch (report.status()) {

            case REGISTER_SUCCESS:
                setInitTextView(report.desc());
                AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_IDLE);
                continueToMainActivity();
                break;

            case REGISTER_FAILURE:
                setInitTextView(report.desc());
                enableLoginEditText();
                enableLoginButton();
                break;

            case REGISTERING:
                setInitTextView(report.desc());
                enableProgressBar();
                disableLoginEditText();
                disableSmsCodeEditText();
                disableLoginButton();
                disableSmsCodeButton();
                break;

            case TOKEN_RETRIEVED:
                disableProgressBar();
                disableInitTextView();

                // Enabling login button if necessary
                EditText loginET = (EditText) findViewById(R.id.LoginNumber);
                CharSequence loginNumber = loginET.getText();
                if (10 == loginNumber.length())
                    findViewById(R.id.login_btn).setEnabled(true);
                break;

            case TOKEN_RETRIEVAL_FAILED:
                disableProgressBar();
                setInitTextView(report.desc());
                disableLoginEditText();
                disableSmsCodeEditText();
                disableLoginButton();
                disableSmsCodeButton();
                break;

            default: // Event not meant for LoginActivity receiver
        }
    }

    /**
     * Saving the instance state - to be used from onPause()
     */
    private void saveInstanceState() {

        // Saving login number
        final EditText loginNumberEditText = ((EditText) findViewById(R.id.LoginNumber));
        if (loginNumberEditText != null) {
            String loginNumber = loginNumberEditText.getText().toString();
            SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.LOGIN_NUMBER, loginNumber);
        }

        // Saving sms code
        final EditText smsCodeEditText = ((EditText) findViewById(R.id.SMSCodeEditText));
        if (smsCodeEditText != null) {
            String smsCode = smsCodeEditText.getText().toString();
            SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.SMS_CODE, smsCode);
        }
    }

    private void restoreInstanceState() {

        Log.i(TAG, "Restoring instance state");

        // Restoring login number
        final EditText loginNumberEditText = ((EditText) findViewById(R.id.LoginNumber));
        String loginNumber = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.LOGIN_NUMBER);
        if (loginNumberEditText != null && loginNumber != null)
            loginNumberEditText.setText(loginNumber);

        // Restoring sms code
        final EditText smsCodeEditText = ((EditText) findViewById(R.id.SMSCodeEditText));
        String smsCode = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.SMS_CODE);
        if(smsCodeEditText!=null && smsCode!=null)
            smsCodeEditText.setText(smsCode);
    }

    private void prepareEventReceiver() {

        if (_eventReceiver == null) {
            _eventReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    EventReport report = (EventReport) intent.getSerializableExtra(Event.EVENT_REPORT);
                    eventReceived(new Event(this, report));
                }
            };
        }

        registerReceiver(_eventReceiver, _eventIntentFilter);
    }


    //endregion
}

