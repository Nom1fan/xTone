package com.ui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.app.AppStateManager;
import com.async_tasks.GetSmsCodeTask;
import com.batch.android.Batch;
import com.data_objects.ActivityRequestCodes;
import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.services.GetTokenIntentService;
import com.services.LogicServerProxyService;
import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;

import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;
import utils.PhoneNumberUtils;


/**
 * Created by Mor on 14/03/2016.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private BroadcastReceiver _eventReceiver;
    private static final IntentFilter _eventIntentFilter = new IntentFilter(Event.EVENT_ACTION);

    //region Constants
    private static final int SERVER_RESPONSE_TIMEOUT = 30*1000; // milliseconds
    //endregion

    //region UI elements
    private EditText _loginNumberEditText;
    private EditText _smsCodeVerEditText;
    private ImageButton _loginBtn;
    private ImageButton _getSmsCodeBtn;
    private ProgressBar _initProgressBar;
    private TextView _initTextView;
    private GetSmsCodeTask _getSmsCodeTask;

    private ImageButton _clearLoginPhoneText;
    private ImageButton _clearLoginSmsText;
    //endregion

    //region Activity methods (onCreate(), onPause(), ...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        initializeLoginUI();
        prepareDrawArrowForTermsAndService();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.i(TAG, "OnStart()");

        Batch.onStart(this);


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

        prepareEventReceiver();

        if(Constants.MY_BATCH_TOKEN(this).equals("")) {

            Intent i = new Intent(this, GetTokenIntentService.class);
            i.setAction(GetTokenIntentService.ACTION_GET_BATCH_TOKEN);
            startService(i);

//            switchToLoadingState(getResources().getString(R.string.initializing),
//                    getResources().getString(R.string.oops_try_again));
        }

        syncUIwithAppState();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK) {
            String errMsg = getResources().getString(R.string.register_failure);
            String registering = getResources().getString(R.string.registering);
            switchToLoadingState(registering, errMsg);
        }

        syncUIwithAppState();
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
        _clearLoginPhoneText = (ImageButton) findViewById(R.id.login_phone_clear);
        _clearLoginPhoneText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _loginNumberEditText.setText("");
            }
        });


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
                    disableSmsCodeEditText();
                    disableGetSmsCodeButton();
                    disableLoginButton();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        enableLoginEditText();
    }

    private void prepareLoginButton() {

        _loginBtn = (ImageButton) findViewById(R.id.login_btn);
        _loginBtn.setEnabled(false);
        _loginBtn.setImageResource(R.drawable.login_icon);
        _loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String smsVerificationCode = _smsCodeVerEditText.getText().toString();
                String loginNumber = _loginNumberEditText.getText().toString();

                Intent intent = new Intent(LoginActivity.this,LoginWithTermsAndServiceActivity.class);
                intent.putExtra(LoginWithTermsAndServiceActivity.SmsCode,smsVerificationCode);
                intent.putExtra(LoginWithTermsAndServiceActivity.LoginNumber,loginNumber);
                startActivityForResult(intent, ActivityRequestCodes.TERMS_OF_SERVICE);

                if(_getSmsCodeTask!=null)
                    _getSmsCodeTask.cancel(true);
            }
        });
        disableLoginButton();
    }

    private void prepareGetSmsCodeButton() {

        _getSmsCodeBtn = (ImageButton) findViewById(R.id.getSmsCode_btn);
        _getSmsCodeBtn.setEnabled(false);
        _getSmsCodeBtn.setImageResource(R.drawable.send_sms_icon_disabled);
        _getSmsCodeBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                String errMsg = getResources().getString(R.string.sms_code_failed);
                String loadingMsg = getResources().getString(R.string.please_wait);
                switchToLoadingState(loadingMsg, errMsg);
                syncUIwithAppState();

                String phoneNumber = _loginNumberEditText.getText().toString();
                getSms(phoneNumber);
            }
        });
        disableGetSmsCodeButton();
    }

    private void prepareSmsCodeVerificationEditText() {

        _smsCodeVerEditText = (EditText) findViewById(R.id.SMSCodeEditText);
        _clearLoginSmsText = (ImageButton) findViewById(R.id.sms_phone_clear);
        _clearLoginSmsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _smsCodeVerEditText.setText("");
            }
        });

        _smsCodeVerEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (4 == s.length()) {
                    enableLoginButton();
                } else
                   disableLoginButton();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        disableSmsCodeEditText();
    }

    private void prepareDrawArrowForTermsAndService() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                Drawable drawArrow = ContextCompat.getDrawable(getApplicationContext(), R.drawable.right_arrow);//getResources().getDrawable( R.drawable.right_arrow );
                if (drawArrow != null) {
                    drawArrow.setAutoMirrored(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enableProgressBar() {

        _initProgressBar.setVisibility(ProgressBar.VISIBLE);
    }

    private void disableProgressBar() {

        _initProgressBar.setVisibility(ProgressBar.GONE);
    }

    private void setInitTextView(String str) {

        Log.i(TAG, "SetInitTextView:" + str);
        _initTextView.setVisibility(View.VISIBLE);
        _initTextView.setText(str);
    }

    private void disableInitTextView() {

        _initTextView.setVisibility(View.INVISIBLE);
    }

    private void enableLoginButton() {

        Log.v(TAG, "Enabling LoginButton");

        _loginBtn.setEnabled(true);
        _loginBtn.setImageResource(R.drawable.login_icon);

    }

    private void disableLoginButton() {

        Log.v(TAG, "Disabling LoginButton");

        _loginBtn.setEnabled(false);
        _loginBtn.setImageResource(R.drawable.login_icon_disabled);
    }

    private void enableLoginEditText() {

        Log.v(TAG, "Enabling LoginEditText");

        _loginNumberEditText.setEnabled(true);
        _loginNumberEditText.setTextColor(ContextCompat.getColor(this, R.color.white));
        _clearLoginPhoneText.setEnabled(true);
    }

    private void disableLoginEditText() {

        Log.v(TAG, "Disabling LoginEditText");

        _loginNumberEditText.setEnabled(false);
        _clearLoginPhoneText.setEnabled(false);
    }

    private void enableSmsCodeEditText() {

        Log.v(TAG, "Enabling SmsCodeEditText");

        _smsCodeVerEditText.setEnabled(true);
        _smsCodeVerEditText.setTextColor(ContextCompat.getColor(this, R.color.white));
        _clearLoginSmsText.setEnabled(true);
    }

    private void disableSmsCodeEditText() {

        Log.v(TAG, "Disabling SmsCodeEditText");

        _smsCodeVerEditText.setEnabled(false);
        _clearLoginSmsText.setEnabled(false);
    }

    private void enableGetSmsCodeButton() {

        Log.v(TAG, "Enabling getSmsButton");

        boolean shouldEnable = true;

        if(_getSmsCodeTask!=null && _getSmsCodeTask.getStatus().equals(AsyncTask.Status.RUNNING))
            shouldEnable = false;

        if(shouldEnable)
        {
            _getSmsCodeBtn.setEnabled(true);
            _getSmsCodeBtn.setImageResource(R.drawable.send_sms_icon);
        }
    }

    private void disableGetSmsCodeButton() {

        Log.v(TAG, "Disabling getSmsButton");

        _getSmsCodeBtn.setEnabled(false);
        _getSmsCodeBtn.setImageResource(R.drawable.send_sms_icon_disabled);

    }

    private void continueToMainActivity() {

        int millisToWait = 1500;
        Log.i(TAG, String.format("Waiting %d milliseconds before continuing to MainActivity", millisToWait));

        try {
            Thread.sleep(millisToWait);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Continuing to MainActivity");
        AppStateManager.setIsLoggedIn(this, true);
        final Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();

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

            case GET_SMS_CODE_SUCCESS:
                AppStateManager.setAppState(this, TAG, AppStateManager.STATE_IDLE);
                syncUIwithAppState();
                _getSmsCodeTask = new GetSmsCodeTask(this, _initTextView, _getSmsCodeBtn);
                _getSmsCodeTask.execute();
                break;

            // Triggered by local loading timeouts
            case LOADING_TIMEOUT:
            case REFRESH_UI:
                setInitTextView(report.desc());
                syncUIwithAppState();
                saveInstanceState();
                restoreInstanceState();
                break;

            default: // Event not meant for LoginActivity receiver
        }
    }

    public static class IncomingSms extends BroadcastReceiver {

        // Get the object of SmsManager
        final SmsManager sms = SmsManager.getDefault();

        public void onReceive(Context context, Intent intent) {

            // Retrieves a map of extended data from the intent.
            final Bundle bundle = intent.getExtras();

            try {

                if (bundle != null) {

                    final Object[] pdusObj = (Object[]) bundle.get("pdus");

                    for (int i = 0; i < pdusObj.length; i++) {

                        SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                        String phoneNumber = currentMessage.getDisplayOriginatingAddress();

                        String senderNum = phoneNumber;
                        String message = currentMessage.getDisplayMessageBody();


                       if (senderNum.toLowerCase().contains("mediacallz")) {
                           SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.SMS_CODE, PhoneNumberUtils.toNumeric(message));
                           BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.REFRESH_UI, null, null));
                       }
                    } // end for loop
                } // bundle is null

            } catch (Exception e) {
                Log.e("SmsReceiver", "Exception smsReceiver" +e);

            }
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
        String savedSmsAutomatically = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.SMS_CODE);
        String smsCode = "";
        if (savedSmsAutomatically.isEmpty())
            smsCode = _smsCodeVerEditText.getText().toString();
        else
            smsCode = savedSmsAutomatically;

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

    private void getSms(String phoneNumber) {

        String interPhoneNumber = PhoneNumberUtils.toValidInternationalPhoneNumber(
                phoneNumber,
                PhoneNumberUtils.Country.IL);

        Constants.MY_ID(this, phoneNumber);

        Intent i = new Intent(this, LogicServerProxyService.class);
        i.setAction(LogicServerProxyService.ACTION_GET_SMS_CODE);
        i.putExtra(LogicServerProxyService.INTERNATIONAL_PHONE, interPhoneNumber);
        startService(i);
    }

    private void disableUnfinishedServerActions() {

        //TODO Mor: Test this code
//        Intent i = new Intent(this, LogicServerProxyService.class);
//        i.setAction(LogicServerProxyService.ACTION_CLOSE_SOCKETS);
//        startService(i);
    }

    private void switchToLoadingState(String loadingMsg, String errMsg) {

        AppStateManager.setAppState(LoginActivity.this, TAG, AppStateManager.createLoadingState(
                new EventReport(EventType.LOADING_TIMEOUT, errMsg), SERVER_RESPONSE_TIMEOUT), loadingMsg
        );
    }

    private void syncUIwithAppState() {

        String appState = getState();

        Log.i(TAG, "Syncing UI with appState:" + appState);

        switch (appState) {

            case AppStateManager.STATE_IDLE:
                stateIdle();
                break;

            case AppStateManager.STATE_LOADING:
                stateLoading();
                break;

            case AppStateManager.STATE_DISABLED:
                stateDisabled();
                break;
        }
    }

    //region UI States
    public void stateIdle() {

        disableProgressBar();
        enableLoginEditText();

        if(10 == _loginNumberEditText.getText().length()) {
            enableSmsCodeEditText();
            enableGetSmsCodeButton();

            if(4 == _smsCodeVerEditText.getText().length()) {
                enableLoginButton();
            }
        }
    }

    public void stateDisabled() {


        String noInternet = getResources().getString(R.string.disconnected);
        setInitTextView(noInternet);
        disableUnfinishedServerActions(); //TODO Mor: Decide how to deal with unanswered requests
        disableProgressBar();
        disableGetSmsCodeButton();
        disableLoginButton();
        disableLoginEditText();
        disableSmsCodeEditText();
    }

    public void stateLoading() {

        String loadingMsg = SharedPrefUtils.getString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE);

        setInitTextView(loadingMsg);
        disableLoginEditText();
        disableSmsCodeEditText();
        disableLoginButton();
        disableGetSmsCodeButton();
        enableProgressBar();
    }

    private String getState() {

        return AppStateManager.getAppState(getApplicationContext());
    }
    //endregion
    //endregion
}

