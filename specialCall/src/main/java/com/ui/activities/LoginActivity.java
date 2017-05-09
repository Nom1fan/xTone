package com.ui.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.app.AppStateManager;
import com.async.tasks.GetSmsCodeTask;
import com.batch.android.Batch;
import com.data.objects.ActivityRequestCodes;
import com.data.objects.Constants;
import com.mediacallz.app.R;
import com.services.GetTokenIntentService;
import com.services.ServerProxyService;
import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;

import com.event.Event;
import com.event.EventReport;
import com.event.EventType;
import com.utils.PhoneNumberUtils;

import static com.crashlytics.android.Crashlytics.log;


/**
 * Created by Mor on 14/03/2016.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private BroadcastReceiver eventReceiver;
    private static final IntentFilter eventIntentFilter = new IntentFilter(Event.EVENT_ACTION);
    private static final int MIN_SMS_CODE = 1000;
    private static final int MAX_SMS_CODE = 9999;

    //region UI elements
    private EditText loginNumberEditText;
    private EditText smsCodeVerEditText;
    private ImageButton loginBtn;
    private ImageButton getSmsCodeBtn;
    private ImageView loginlogo;
    private ProgressBar initProgressBar;
    private TextView initTextView;
    private GetSmsCodeTask getSmsCodeTask;
    private static boolean sentFromReceiver = false;
    private ImageButton clearLoginPhoneText;
    private ImageButton clearLoginSmsText;
    private  String[] smsPermissions = {Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS };
    //endregion

    //region Activity methods (onCreate(), onPause(), ...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log(Log.INFO, TAG, "onCreate()");

        initializeLoginUI();
    }

    @Override
    protected void onStart() {
        super.onStart();

        log(Log.INFO, TAG, "OnStart()");

        Batch.onStart(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        log(Log.INFO, TAG, "onPause()");

        if (eventReceiver != null) {
            try {
                unregisterReceiver(eventReceiver);
            } catch (Exception ex) {
                log(Log.ERROR, TAG, ex.getMessage());
            }
        }
        saveInstanceState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        log(Log.INFO, TAG, "OnResume()");

        restoreInstanceState();
        prepareEventReceiver();

        if (Constants.MY_BATCH_TOKEN(this).equals("")) {
            Intent i = new Intent(this, GetTokenIntentService.class);
            i.setAction(GetTokenIntentService.ACTION_GET_BATCH_TOKEN);
            startService(i);
        }

        syncUIwithAppState();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        syncUIwithAppState();
    }
    //endregion

    //region UI methods
    private void initializeLoginUI() {

        setContentView(R.layout.loginuser);

        prepareLoginLogo();
        prepareLoginNumberEditText();
        prepareSmsCodeVerificationEditText();
        prepareLoginButton();
        prepareInitTextView();
        prepareInitProgressBar();
        prepareGetSmsCodeButton();

        if (10 == SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.LOGIN_NUMBER).length()) {
            visibleSmsButtons();
        }

    }

    private void visibleSmsButtons() {
        loginBtn.setVisibility(View.VISIBLE);
        smsCodeVerEditText.setVisibility(View.VISIBLE);
        clearLoginSmsText.setVisibility(View.VISIBLE);
    }

    private void prepareLoginLogo() {
        loginlogo = (ImageView) findViewById(R.id.loginlogo);
        loginlogo.setEnabled(true);

        loginlogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                visibleSmsButtons();

            }
        });
    }

    private void prepareInitProgressBar() {
        initProgressBar = (ProgressBar) findViewById(R.id.initProgressBar);
        initProgressBar.setVisibility(ProgressBar.GONE);
    }

    private void prepareInitTextView() {
        initTextView = (TextView) findViewById(R.id.initTextView);
    }

    private void prepareLoginNumberEditText() {
        loginNumberEditText = (EditText) findViewById(R.id.LoginNumber);
        clearLoginPhoneText = (ImageButton) findViewById(R.id.login_phone_clear);
        clearLoginPhoneText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginNumberEditText.setText("");

                invisibleSmsButtons();

            }
        });

        loginNumberEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (0 == s.length())
                    clearLoginPhoneText.setImageResource(0);
                else
                    clearLoginPhoneText.setImageResource(R.drawable.clear_btn_anim);

                if (10 == s.length()) {

                    if (PhoneNumberUtils.isValidPhoneNumber(s.toString())) {

                        String token = Constants.MY_BATCH_TOKEN(getApplicationContext());
                        if (token != null && !token.equals("")) {
                            enableGetSmsCodeButton();
                            enableSmsCodeEditText();

                            if (4 == smsCodeVerEditText.getText().toString().length()) {
                                enableLoginButton();
                                visibleSmsButtons();

                            }
                        }
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

    private void invisibleSmsButtons() {
        loginBtn.setVisibility(View.INVISIBLE);
        smsCodeVerEditText.setVisibility(View.INVISIBLE);
        clearLoginSmsText.setVisibility(View.INVISIBLE);
    }

    private void prepareLoginButton() {

        loginBtn = (ImageButton) findViewById(R.id.login_btn);
        if (loginBtn != null) {
            loginBtn.setEnabled(false);
            loginBtn.setImageResource(R.drawable.login_btn_anim);
            loginBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        confirmLogin();
                }
            });
        }
        disableLoginButton();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_SMS_PERMISSION: {
                    sendSMSConfirm();
            break;
            }
        }
    }

    private void confirmLogin(){
        String smsVerificationCode = smsCodeVerEditText.getText().toString();
        String loginNumber = loginNumberEditText.getText().toString();

        Intent intent = new Intent(LoginActivity.this, LoginWithTermsAndServiceActivity.class);
        intent.putExtra(LoginWithTermsAndServiceActivity.SMS_CODE, smsVerificationCode);
        intent.putExtra(LoginWithTermsAndServiceActivity.LOGIN_NUMBER, loginNumber);
        startActivityForResult(intent, ActivityRequestCodes.TERMS_OF_SERVICE);

        if (getSmsCodeTask != null)
            getSmsCodeTask.cancel(true);

    }
    private void sendSMSConfirm(){

        String timeoutMsg = getResources().getString(R.string.sms_code_failed);
        String loadingMsg = getResources().getString(R.string.please_wait);
        AppStateManager.setLoadingState(LoginActivity.this, TAG, loadingMsg, timeoutMsg);

        String phoneNumber = loginNumberEditText.getText().toString();
        getSms(phoneNumber);

        visibleSmsButtons();

    }

    private void prepareGetSmsCodeButton() {

        getSmsCodeBtn = (ImageButton) findViewById(R.id.getSmsCode_btn);
        if (getSmsCodeBtn != null) {
            getSmsCodeBtn.setEnabled(false);
            getSmsCodeBtn.setImageResource(R.drawable.send_sms_icon_disabled);
            getSmsCodeBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        ActivityCompat.requestPermissions(LoginActivity.this, smsPermissions, Constants.MY_PERMISSIONS_SMS_PERMISSION);
                    else
                        sendSMSConfirm();
                }
            });
        }
        disableGetSmsCodeButton();
    }

    private void prepareSmsCodeVerificationEditText() {
        smsCodeVerEditText = (EditText) findViewById(R.id.SMSCodeEditText);
        clearLoginSmsText = (ImageButton) findViewById(R.id.sms_phone_clear);
        clearLoginSmsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                smsCodeVerEditText.setText("");
            }
        });

        smsCodeVerEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (0 == s.length())
                    clearLoginSmsText.setImageResource(0);
                else
                    clearLoginSmsText.setImageResource(R.drawable.clear_btn_anim);

                if (isSmsCodeValid()) {
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

    private void enableProgressBar() {
        initProgressBar.setVisibility(ProgressBar.VISIBLE);
        loginlogo.setVisibility(View.INVISIBLE);
    }

    private void disableProgressBar() {
        initProgressBar.setVisibility(ProgressBar.GONE);
        loginlogo.setVisibility(View.VISIBLE);
    }

    private void setInitTextView(String str) {
        if (str != null) {
            log(Log.INFO, TAG, "SetInitTextView:" + str);
            initTextView.setVisibility(View.VISIBLE);
            initTextView.setText(str);
        }
    }

    private void disableInitTextView() {
        initTextView.setVisibility(View.INVISIBLE);
    }

    private void enableLoginButton() {

        Log.d(TAG, "Enabling LoginButton");

        loginBtn.setEnabled(true);
        loginBtn.setImageResource(R.drawable.login_btn_anim);

    }

    private void disableLoginButton() {

        Log.d(TAG, "Disabling LoginButton");

        loginBtn.setEnabled(false);
        loginBtn.setImageResource(R.drawable.login_icon_disabled);
    }

    private void enableLoginEditText() {

        Log.d(TAG, "Enabling LoginEditText");

        loginNumberEditText.setEnabled(true);
        loginNumberEditText.setTextColor(ContextCompat.getColor(this, R.color.white));
        clearLoginPhoneText.setEnabled(true);
    }

    private void disableLoginEditText() {
        Log.d(TAG, "Disabling LoginEditText");

        loginNumberEditText.setEnabled(false);
        clearLoginPhoneText.setEnabled(false);
    }

    private void enableSmsCodeEditText() {
        Log.d(TAG, "Enabling SmsCodeEditText");

        smsCodeVerEditText.setEnabled(true);
        smsCodeVerEditText.setTextColor(ContextCompat.getColor(this, R.color.white));
        clearLoginSmsText.setEnabled(true);
    }

    private void disableSmsCodeEditText() {
        Log.d(TAG, "Disabling SmsCodeEditText");

        smsCodeVerEditText.setEnabled(false);
        clearLoginSmsText.setEnabled(false);
    }

    private void enableGetSmsCodeButton() {
        Log.d(TAG, "Enabling getSmsButton");

        boolean shouldEnable = true;

        if (getSmsCodeTask != null && getSmsCodeTask.getStatus().equals(AsyncTask.Status.RUNNING))
            shouldEnable = false;

        if (shouldEnable) {
            getSmsCodeBtn.setEnabled(true);
            getSmsCodeBtn.setImageResource(R.drawable.send_sms_anim);
        }
    }

    private void disableGetSmsCodeButton() {
        Log.d(TAG, "Disabling getSmsButton");

        getSmsCodeBtn.setEnabled(false);
        getSmsCodeBtn.setImageResource(R.drawable.send_sms_icon_disabled);
    }

    private void continueToMainActivity() {
        int millisToWait = 1500;
        log(Log.INFO, TAG, String.format("Waiting %d milliseconds before continuing to MainActivity", millisToWait));

        try {
            Thread.sleep(millisToWait);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log(Log.INFO, TAG, "Continuing to MainActivity");
        AppStateManager.setIsLoggedIn(this, true);
        final Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
    //endregion

    //region Assisting methods (eventReceived(), ...)
    public void eventReceived(Event event) {
        final EventReport report = event.report();
        log(Log.INFO, TAG, "Receiving event:" + report.status());

        switch (report.status()) {

            case REGISTER_SUCCESS:
                setInitTextView(getResources().getString(R.string.register_success));
                AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_IDLE);
                continueToMainActivity();
                break;

            case GET_SMS_CODE_SUCCESS:
                AppStateManager.setAppState(this, TAG, AppStateManager.STATE_IDLE);
                syncUIwithAppState();
                getSmsCodeTask = new GetSmsCodeTask(this, initTextView, getSmsCodeBtn);
                getSmsCodeTask.execute();
                break;

            case REFRESH_UI:
                setInitTextView(report.desc());
                syncUIwithAppState();
                saveInstanceState();
                restoreInstanceState();
                if (isLoginNumberValid() && isSmsCodeValid() && sentFromReceiver) {
                    visibleSmsButtons();
                    loginBtn.performClick();
                    sentFromReceiver = false;
                }
                break;

            default: // Event not meant for LoginActivity receiver
        }
    }

    public static class IncomingSms extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {

            // Retrieves a map of extended data from the intent.
            final Bundle bundle = intent.getExtras();

            try {

                if (bundle != null) {

                    final Object[] pdusObj = (Object[]) bundle.get("pdus");

                    if (pdusObj != null) {
                        for (Object aPdusObj : pdusObj) {
                            SmsMessage currentMessage;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                String format = bundle.getString("format");
                                currentMessage = SmsMessage.createFromPdu((byte[]) aPdusObj, format);
                            } else {
                                currentMessage = SmsMessage.createFromPdu((byte[]) aPdusObj);
                            }

                            String phoneNumber = currentMessage.getDisplayOriginatingAddress();

                            String message = currentMessage.getDisplayMessageBody();

                            if (phoneNumber.toLowerCase().contains("mediacallz")) {
                                SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.AUTO_SMS_CODE, PhoneNumberUtils.toNumeric(message));
                                SharedPrefUtils.setBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.AUTO_SMS_CODE_RECEIVED, true);
                                sentFromReceiver = true;
                                BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.REFRESH_UI));
                            }
                        } // end for loop
                    }
                } // bundle is null

            } catch (Exception e) {
                log(Log.ERROR, "SmsReceiver", "Exception smsReceiver" + e);
            }
        }
    }

    private boolean isSmsCodeValid() {
        String sSmsCode = smsCodeVerEditText.getText().toString();
        if(sSmsCode.isEmpty()) {
            return false;
        }

        Integer smsCode = Integer.valueOf(sSmsCode);
        return 4 == smsCodeVerEditText.getText().length() && smsCode >= MIN_SMS_CODE && smsCode <= MAX_SMS_CODE;
    }

    private boolean isLoginNumberValid() {
        return 10 == loginNumberEditText.getText().length();
    }


    /**
     * Saving the instance state - to be used from onPause()
     */
    private void saveInstanceState() {
        // Saving login number
        String loginNumber = loginNumberEditText.getText().toString();
        SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.LOGIN_NUMBER, loginNumber);

        // Saving sms code
        String smsCode;
        if (SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.AUTO_SMS_CODE_RECEIVED)) {
            smsCode = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.AUTO_SMS_CODE);
            SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.AUTO_SMS_CODE_RECEIVED, false);
        } else
            smsCode = smsCodeVerEditText.getText().toString();

        SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.SMS_CODE, smsCode);
    }

    private void restoreInstanceState() {
        log(Log.INFO, TAG, "Restoring instance state");

        // Restoring login number
        String loginNumber = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.LOGIN_NUMBER);
        if (loginNumberEditText != null && loginNumber != null)
            loginNumberEditText.setText(loginNumber);

        // Restoring sms code
        String smsCode = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.SMS_CODE);
        if (smsCodeVerEditText != null && smsCode != null)
            smsCodeVerEditText.setText(smsCode);
    }

    private void prepareEventReceiver() {
        if (eventReceiver == null) {
            eventReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    EventReport report = (EventReport) intent.getSerializableExtra(Event.EVENT_REPORT);
                    eventReceived(new Event(this, report));
                }
            };
        }

        registerReceiver(eventReceiver, eventIntentFilter);
    }

    private void getSms(String phoneNumber) {
        String interPhoneNumber = PhoneNumberUtils.toValidInternationalPhoneNumber(
                phoneNumber,
                PhoneNumberUtils.Country.IL);

        Constants.MY_ID(this, phoneNumber);

        Intent i = new Intent(this, ServerProxyService.class);
        i.setAction(ServerProxyService.ACTION_GET_SMS_CODE);
        i.putExtra(ServerProxyService.INTERNATIONAL_PHONE, interPhoneNumber);
        startService(i);
    }

    private void syncUIwithAppState() {
        String appState = getState();

        log(Log.INFO, TAG, "Syncing UI with appState:" + appState);

        switch (appState) {

            case AppStateManager.STATE_IDLE:
                stateIdle();
                break;

            case AppStateManager.STATE_LOADING:
                stateLoading();
                break;
        }
    }

    //region UI States
    public void stateIdle() {
        disableProgressBar();
        enableLoginEditText();

        if (isLoginNumberValid()) {
            enableSmsCodeEditText();
            enableGetSmsCodeButton();

            if (isSmsCodeValid()) {
                enableLoginButton();
            }
        }
    }

    public void stateDisabled() {
        String noInternet = getResources().getString(R.string.disconnected);
        setInitTextView(noInternet);
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

