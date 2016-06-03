package com.ui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.AppStateManager;
import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.services.LogicServerProxyService;

import EventObjects.Event;
import EventObjects.EventReport;


/**
 * Created by Mor on 14/03/2016.
 */
public class LoginWithTermsAndServiceActivity extends AppCompatActivity {

    private static final String TAG = LoginWithTermsAndServiceActivity.class.getSimpleName();
    public static final String SmsCode = "SMS_CODE";
    public static final String LoginNumber = "LOGIN_NUMBER";

    private BroadcastReceiver _eventReceiver;
    private IntentFilter _eventIntentFilter = new IntentFilter(Event.EVENT_ACTION);

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
        if (_eventReceiver != null) {
            try {
                unregisterReceiver(_eventReceiver);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
    }

    @Override
       protected void onResume() {
        super.onResume();

        Log.i(TAG, "OnResume()");
        prepareEventReceiver();

    }
    //endregion

    //region UI methods
    private void initializeTermsOfserviceUI() {

        setContentView(R.layout.login_termsofservice);

        Button cancel = (Button) findViewById(R.id.cancel_login);
        cancel.setOnClickListener(new View.OnClickListener() {
                                      @Override
                                      public void onClick(View v) {

                                          finish();
                                      }
                                  }
        );

        Button login = (Button) findViewById(R.id.login_continue);
        login.setOnClickListener(new View.OnClickListener() {
                                      @Override
                                      public void onClick(View v) {

                                          Constants.MY_ID(getApplicationContext(), _loginNumber);

                                          Intent i = new Intent(getApplicationContext(), LogicServerProxyService.class);
                                          i.setAction(LogicServerProxyService.ACTION_REGISTER);
                                          i.putExtra(LogicServerProxyService.SMS_CODE, Integer.parseInt(_smsVerificationCode));
                                          getApplicationContext().startService(i);
                                      }
                                  }
        );


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
                                     }
        );



        ImageView arrow = (ImageView) findViewById(R.id.arrow);
        arrow.bringToFront();
        arrow.setVisibility(View.VISIBLE);



    }

    //endregion

    //region Assisting methods (eventReceived(), ...)

    /**
     * Saving the instance state - to be used from onPause()
     */

    public void eventReceived(Event event) {

        final EventReport report = event.report();
        Log.i(TAG, "Receiving event:" + report.status());

        switch (report.status()) {

            case REGISTER_SUCCESS:
                AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_IDLE);
                continueToMainActivity();
                break;

            case REGISTER_FAILURE:
                finish();
                break;


            default: // Event not meant for LoginActivity receiver
        }
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
        final Intent i = new Intent(LoginWithTermsAndServiceActivity.this, MainActivity.class);
        startActivity(i);
        finish();

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

