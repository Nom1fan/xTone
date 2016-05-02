package com.ui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
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
import com.async_tasks.GetSmsCodeTask;
import com.batch.android.Batch;
import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.services.GetTokenIntentService;
import com.services.LogicServerProxyService;
import com.utils.SharedPrefUtils;

import EventObjects.Event;
import EventObjects.EventReport;


/**
 * Created by Mor on 14/03/2016.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private BroadcastReceiver _eventReceiver;
    private IntentFilter _eventIntentFilter = new IntentFilter(Event.EVENT_ACTION);

    //region UI elements
    private EditText _loginNumberEditText;
    private EditText _smsCodeVerEditText;
    private Button _loginBtn;
    private Button _getSmsCodeBtn;
    private ProgressBar _initProgressBar;
    private TextView _initTextView;
    private GetSmsCodeTask _getSmsCodeTask;
    //endregion

    //region Activity methods (onCreate(), onPause(), ...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        initializeLoginUI();

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
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "OnResume()");

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
        prepareSmsCodeVerificationEditText();
        prepareLoginButton();
        prepareInitTextView();
        prepareInitProgressBar();
        prepareGetSmsCodeButton();

    }

    private void prepareInitProgressBar() {

        _initProgressBar = (ProgressBar)findViewById(R.id.initProgressBar);
        _initProgressBar.setVisibility(ProgressBar.GONE);
    }

    private void prepareInitTextView() {

        _initTextView = (TextView)findViewById(R.id.initTextView);
    }

    private void prepareLoginNumberEditText() {

        _loginNumberEditText = (EditText) findViewById(R.id.LoginNumber);
        _loginNumberEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (10 == s.length()) {

                    String token = Constants.MY_BATCH_TOKEN(getApplicationContext());
                    if (token != null && !token.equals("")) {
                        enableGetSmsCodeButton();
                        enableSmsCodeEditText();

                        if (4 == _smsCodeVerEditText.getText().toString().length())
                            enableLoginButton();
                    }
                } else {
                    enableSmsCodeEditText();
                    disableGetSmsCodeButton();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void prepareLoginButton() {

        _loginBtn = (Button) findViewById(R.id.login_btn);
        _loginBtn.setEnabled(false);
        _loginBtn.setText(getResources().getString(R.string.login));
        _loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String smsVerificationCode = _smsCodeVerEditText.getText().toString();
                String loginNumber = _loginNumberEditText.getText().toString();

                Constants.MY_ID(getApplicationContext(), loginNumber);

                Intent i = new Intent(getApplicationContext(), LogicServerProxyService.class);
                i.setAction(LogicServerProxyService.ACTION_REGISTER);
                i.putExtra(LogicServerProxyService.SMS_CODE, Integer.parseInt(smsVerificationCode));
                startService(i);

                if(_getSmsCodeTask!=null)
                    _getSmsCodeTask.cancel(true);
            }
        });
    }

    private void prepareGetSmsCodeButton() {


        _getSmsCodeBtn = (Button) findViewById(R.id.getSmsCode_btn);
        _getSmsCodeBtn.setEnabled(false);
        _getSmsCodeBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                _getSmsCodeTask = new GetSmsCodeTask(getApplicationContext(), _initTextView, _getSmsCodeBtn);
                String phoneNumber = _loginNumberEditText.getText().toString();
                _getSmsCodeTask.execute(phoneNumber);
            }
        });
    }

    private void prepareSmsCodeVerificationEditText() {

        _smsCodeVerEditText = (EditText) findViewById(R.id.SMSCodeEditText);
        enableSmsCodeEditText();
        _smsCodeVerEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (4 == s.length() && 10 == _loginNumberEditText.getText().toString().length()) {
                    enableLoginButton();
                } else
                   disableLoginButton();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void enableProgressBar() {

        _initProgressBar.setVisibility(ProgressBar.VISIBLE);
    }

    private void disableProgressBar() {

        _initProgressBar.setVisibility(ProgressBar.GONE);
    }

    private void setInitTextView(String str) {

        _initTextView.setVisibility(View.VISIBLE);
        _initTextView.setText(str);
    }

    private void disableInitTextView() {

        _initTextView.setVisibility(View.INVISIBLE);
    }

    private void enableLoginButton() {

        _loginBtn.setEnabled(true);
    }

    private void disableLoginButton() {

        _loginBtn.setEnabled(false);
    }

    private void enableLoginEditText() {

        _loginNumberEditText.setEnabled(true);
    }

    private void disableLoginEditText() {

        _loginNumberEditText.setEnabled(false);
    }

    private void enableSmsCodeEditText() {

        _smsCodeVerEditText.setEnabled(true);
    }

    private void disableSmsCodeEditText() {

        _smsCodeVerEditText.setEnabled(false);
    }

    private void enableGetSmsCodeButton() {

        boolean shouldEnable = true;

        if(_getSmsCodeTask!=null && _getSmsCodeTask.getStatus().equals(AsyncTask.Status.RUNNING))
            shouldEnable = false;

        if(shouldEnable)
            _getSmsCodeBtn.setEnabled(true);
    }

    private void disableGetSmsCodeButton() {

        Log.i(TAG, "Disabling getSmsButton");
        _getSmsCodeBtn.setEnabled(false);

    }

    private void continueToMainActivity() {

        int millisToWait = 1500;
        Log.i(TAG, String.format("Waiting %d milliseconds before continuing to MainActivity", millisToWait));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Continuing to MainActivity");
                final Intent i = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        }, millisToWait);
    }
    //endregion

    //region Assisting methods (eventReceived(), ...)

    public void eventReceived(Event event) {

        final EventReport report = event.report();
        Log.i(TAG, "Receiving event:" + report.status());

        switch (report.status()) {

            case REGISTER_SUCCESS:
                setInitTextView(getResources().getString(R.string.register_success));
                AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_IDLE);
                continueToMainActivity();
                break;

            case REGISTER_FAILURE:
                disableProgressBar();
                setInitTextView(getResources().getString(R.string.register_failure));
                enableSmsCodeEditText();
                enableGetSmsCodeButton();
                enableLoginEditText();
                enableLoginButton();
                break;

            case REGISTERING:
                setInitTextView(report.desc());
                enableProgressBar();
                disableLoginEditText();
                disableSmsCodeEditText();
                disableLoginButton();
                disableGetSmsCodeButton();
                break;

            case TOKEN_RETRIEVED:
                disableProgressBar();
                disableInitTextView();
                break;

            case TOKEN_RETRIEVAL_FAILED:
                disableProgressBar();
                setInitTextView(report.desc());
                disableLoginEditText();
                disableSmsCodeEditText();
                disableLoginButton();
                disableGetSmsCodeButton();
                break;

            case GET_SMS_CODE_FAILED:
                disableProgressBar();
                setInitTextView(getResources().getString(R.string.sms_code_failed));
                break;

            default: // Event not meant for LoginActivity receiver
        }
    }

    /**
     * Saving the instance state - to be used from onPause()
     */
    private void saveInstanceState() {

        // Saving login number
        String loginNumber = _loginNumberEditText.getText().toString();
        SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.LOGIN_NUMBER, loginNumber);

        // Saving sms code
        String smsCode = _smsCodeVerEditText.getText().toString();
        SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.SMS_CODE, smsCode);
    }

    private void restoreInstanceState() {

        Log.i(TAG, "Restoring instance state");

        // Restoring login number
        String loginNumber = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.LOGIN_NUMBER);
        if (_loginNumberEditText != null && loginNumber != null)
            _loginNumberEditText.setText(loginNumber);

        // Restoring sms code
        String smsCode = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.SMS_CODE);
        if(_smsCodeVerEditText!=null && smsCode!=null)
            _smsCodeVerEditText.setText(smsCode);
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

