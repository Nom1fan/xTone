package com.ui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.app.AppStateManager;
import com.batch.android.Batch;
import com.data_objects.Constants;
import com.data_objects.Contact;
import com.data_objects.SnackbarData;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.services.IncomingService;
import com.services.LogicServerProxyService;
import com.services.OutgoingService;
import com.services.StorageServerProxyService;
import com.special.app.R;
import com.ui.components.AutoCompletePopulateListAsyncTask;
import com.utils.BitmapUtils;
import com.utils.BroadcastUtils;
import com.utils.ContactsUtils;
import com.utils.LUT_Utils;
import com.utils.PhoneNumberUtils;
import com.utils.SharedPrefUtils;

import java.util.ArrayList;
import java.util.List;

import DataObjects.SpecialMediaType;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static boolean wasFileChooser = false; // todo try to remove this static shit that also is used by selectmedia class
    private final String shareBody = String.valueOf(R.string.invite);
    CustomDrawerAdapter mAdapter;
    List<DrawerItem> dataList;
    private String _myPhoneNumber = "";
    private String _destPhoneNumber = "";
    private String _destName = "";
    private int _SMType;
    private ProgressBar _pFetchUserBar;
    private ProgressBar _pBar;
    private BroadcastReceiver _eventReceiver;
    private IntentFilter _eventIntentFilter = new IntentFilter(Event.EVENT_ACTION);
    private AutoCompleteTextView _autoCompleteTextViewDestPhone;
    private int _randomPIN = 0;
    private ListView _DrawerList;
    private ActionBarDrawerToggle _mDrawerToggle;
    private DrawerLayout _mDrawerLayout;

    //region Activity methods
    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart()");

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
    protected void onStop() {
        Batch.onStop(this);

        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");

        String appState = getState();
        Log.i(TAG, "App State:" + appState);

        // Starting service responsible for incoming media callz
        Intent incomingServiceIntent = new Intent(this, IncomingService.class);
        incomingServiceIntent.setAction(IncomingService.ACTION_START);
        startService(incomingServiceIntent);

        // Starting service responsible for outgoing media callz
        Intent outgoingServiceIntent = new Intent(this, OutgoingService.class);
        outgoingServiceIntent.setAction(OutgoingService.ACTION_START);
        startService(outgoingServiceIntent);

        Log.i(TAG, "startService: IncomingService");

        syncUIwithAppState();

        if (!appState.equals(AppStateManager.STATE_LOGGED_OUT)) {

            // Taking Focus from AutoCompleteTextView in the end, so he won't pop up :) added also focus capabilities to the MainActivity Layout XML
            findViewById(R.id.mainActivity).requestFocus();

            prepareEventReceiver();

            // ASYNC TASK To Populate all contacts , it can take long time and it delays the UI
            new AutoCompletePopulateListAsyncTask(this, _autoCompleteTextViewDestPhone).execute();

            if (appState.equals(AppStateManager.STATE_DISABLED)) {

                _myPhoneNumber = Constants.MY_ID(getApplicationContext());
                initializeConnection();

            }

            restoreInstanceState();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy()");
        Batch.onDestroy(this);
        super.onDestroy();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        Batch.onNewIntent(this, intent);

        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        dataList = new ArrayList<DrawerItem>();
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.custom_action_bar);

        if (getState().equals(AppStateManager.STATE_LOGGED_OUT)) {
            initializeLoginUI();

        } else {
            initializeUI();

            if (getState().equals(AppStateManager.STATE_LOGGED_IN)) {
                stateIdle();
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (_mDrawerLayout != null)
            _mDrawerToggle.syncState();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == ActivityRequestCodes.SELECT_MEDIA) {
                writeInfoSnackBar(data.getStringExtra("msg"), Color.RED, Snackbar.SnackbarDuration.LENGTH_INDEFINITE);
            }

            if (requestCode == ActivityRequestCodes.SELECT_CONTACT) {
                try {
                    if (data != null) {
                        Uri uri = data.getData();
                        Contact contact = ContactsUtils.getContact(uri, getApplicationContext());
                        saveInstanceState(contact.get_name(), PhoneNumberUtils.toValidPhoneNumber(contact.get_phoneNumber()));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    @Override
    public boolean onKeyDown ( int keyCode, KeyEvent e) {  // hard menu key will open and close the drawer menu also
        if (keyCode == KeyEvent.KEYCODE_MENU) {

            if (_mDrawerLayout != null) {
                if (!_mDrawerLayout.isDrawerOpen(GravityCompat.START))
                    _mDrawerLayout.openDrawer(GravityCompat.START);
                else
                    _mDrawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (_mDrawerToggle != null)
            if (_mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }

        switch (item.getItemId()) {
            case R.id.action_settings:
                appSettings();
                break;
            case R.id.action_share:
                saveInstanceState();
                shareUs();
                break;
            default:
                saveInstanceState();
                Intent o = new Intent();
                o.setClass(getApplicationContext(), Settings.class);
                startActivity(o);
                break;
        }
        return true;
    }
    //endregion

  /*  @Override     //  the menu with the 3 dots on the right, on the top action bar, to enable it uncomment this.
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }*/

    private void selectMedia(int code) {

        _SMType = code;
                     /* Create an intent that will start the main activity. */
        Intent mainIntent = new Intent(MainActivity.this,
                SelectMediaActivity.class);
        mainIntent.putExtra("SpecialMediaType", _SMType);
        mainIntent.putExtra("DestinationNumber", _destPhoneNumber);
        mainIntent.putExtra("DestinationName", _destName);
        //SplashScreen.this.startActivity(mainIntent);
        startActivityForResult(mainIntent, ActivityRequestCodes.SELECT_MEDIA);

                     /* Apply our splash exit (fade out) and main
                        entry (fade in) animation transitions. */
        overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);// open drawer animation
    }

    public void onClick(View v) {

        // Saving instance state
        saveInstanceState();

        int id = v.getId();
        if (id == R.id.CallNow) {

            launchDialer(_destPhoneNumber);

        } else if (id == R.id.selectMediaBtn) {

            openCallerMediaMenu();

        } else if (id == R.id.selectProfileMediaBtn) {

            openProfileMediaMenu();

        } else if (id == R.id.selectContactBtn) {

            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            startActivityForResult(intent, ActivityRequestCodes.SELECT_CONTACT);

        } else if (id == R.id.clear) {

            AutoCompleteTextView textViewToClear = (AutoCompleteTextView) findViewById(R.id.CallNumber);
            textViewToClear.setText("");

        } else if (id == R.id.inviteButton) {

            EditText callNumber = (EditText) findViewById(R.id.CallNumber);
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(callNumber.getText().toString(), null, shareBody, null, null);
                writeInfoSnackBar("Invitation Sent To: " + callNumber.getText().toString());

            } catch (Exception ex) {
                writeErrStatBar(ex.getMessage());
            }
        } else if (id == R.id.login_btn) {

            String myVerificationcode = ((EditText) findViewById(R.id.SMSCodeEditText)).getText().toString();
            //if (myVerificationcode.equals(String.valueOf(_randomPIN))){    // NEED TO FIND A SMS GATEWAY FIRST

            _myPhoneNumber = ((EditText) findViewById(R.id.LoginNumber))
                    .getText().toString();

            SharedPrefUtils.setString(getApplicationContext(),
                    SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER, _myPhoneNumber);

            initializeConnection();

            initializeUI();

            stateIdle();
        }
    }

    private void initializeLoginUI() {

        setContentView(R.layout.loginuser);

        if (!Constants.MY_BATCH_TOKEN(getApplicationContext()).equals("")) {
            findViewById(R.id.initProgressBar).setVisibility(ProgressBar.INVISIBLE);
            findViewById(R.id.initTextView).setVisibility(TextView.INVISIBLE);
        }

        prepareLoginNumberEditText();
        prepareLoginButton();
        prepareGetSmsCodeButton();
        prepareSmsCodeVerificationEditText();

    }

    private void initializeUI() {

        setContentView(R.layout.activity_main);

        enableHamburgerIconWithSlideMenu();

        prepareAutoCompleteTextViewDestPhoneNumber();

        findViewById(R.id.CallNow).setOnClickListener(this);
        findViewById(R.id.selectMediaBtn).setOnClickListener(this);
        findViewById(R.id.selectContactBtn).setOnClickListener(this);
        findViewById(R.id.selectProfileMediaBtn).setOnClickListener(this);;
        findViewById(R.id.clear).setOnClickListener(this);;
        findViewById(R.id.inviteButton).setOnClickListener(this);
    }

    public void eventReceived(Event event) {

        final EventReport report = event.report();

        switch (report.status()) {

            case USER_REGISTERED_FALSE:
                userStatusUnregistered();
                break;

            case REFRESH_UI:
                SnackbarData data = (SnackbarData) report.data();
                syncUIwithAppState();

                if (data != null)
                    handleSnackBar((SnackbarData) report.data());
                break;

            case DISPLAY_ERROR:
                writeErrStatBar(report.desc());
                break;

            case DISPLAY_MESSAGE:
                writeInfoSnackBar(report.desc());
                break;

            case TOKEN_RETRIEVED:
                findViewById(R.id.initProgressBar).setVisibility(ProgressBar.INVISIBLE);
                findViewById(R.id.initTextView).setVisibility(TextView.INVISIBLE);
                EditText loginET = (EditText) findViewById(R.id.LoginNumber);
                CharSequence loginNumber = loginET.getText();
                if (10 == loginNumber.length())
                    findViewById(R.id.login_btn).setEnabled(true);
                break;

            default:
                Log.e(TAG, "Undefined event status on EventReceived");
        }
    }

	/* -------------- Assisting methods -------------- */

    /**
     * Saving the instance state - to be used from onPause()
     */
    private void saveInstanceState() {

        // Saving destination number
        final AutoCompleteTextView ed_destinationNumber = ((AutoCompleteTextView) findViewById(R.id.CallNumber));
        if (ed_destinationNumber != null) {
            _destPhoneNumber = ed_destinationNumber.getText().toString();
            SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER, _destPhoneNumber);
        }

        // Saving destination name
        final TextView ed_destinationName = ((TextView) findViewById(R.id.destName));
        if (ed_destinationName != null) {
            _destName = ed_destinationName.getText().toString();
            SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME, _destName);
        }
    }

    /**
     * Saving the instance state - Should be used from onActivityResult.SELECT_CONTACT
     *
     * @param destName   The destination name to be saved
     * @param destNumber The destination number to be saved
     */
    private void saveInstanceState(String destName, String destNumber) {

        // Saving destination number
        _destPhoneNumber = destNumber;
        SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER, _destPhoneNumber);


        // Saving destination name
        _destName = destName;
        SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME, _destName);
    }

    private void clearMedia(SpecialMediaType spMediaType) {

        Intent i = new Intent(getApplicationContext(), StorageServerProxyService.class);
        i.setAction(StorageServerProxyService.ACTION_CLEAR_MEDIA);
        i.putExtra(StorageServerProxyService.DESTINATION_ID, _destPhoneNumber);
        i.putExtra(StorageServerProxyService.SPECIAL_MEDIA_TYPE, spMediaType);
        getApplicationContext().startService(i);
    }

    private void restoreInstanceState() {

        Log.i(TAG, "Restoring instance state");

        // Restoring destination number
        final AutoCompleteTextView ed_destinationNumber =
                (AutoCompleteTextView) findViewById(R.id.CallNumber);
        String destNumber = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER);
        if (ed_destinationNumber != null && destNumber != null)
            ed_destinationNumber.setText(destNumber);

        // Restoring destination name
        _destName = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME);
        setDestNameTextView();

        // Restoring my phone number
        _myPhoneNumber = Constants.MY_ID(getApplicationContext());

    }

    private void setDestNameTextView() {

        final TextView tv_destName =
                (TextView) findViewById(R.id.destName);
        if (tv_destName != null && _destName != null)
            tv_destName.setText(_destName);
    }

    private void prepareEventReceiver() {

        if(_eventReceiver==null) {
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

    private void prepareAutoCompleteTextViewDestPhoneNumber() {

        _autoCompleteTextViewDestPhone = (AutoCompleteTextView) findViewById(R.id.CallNumber);
        _autoCompleteTextViewDestPhone.setRawInputType(InputType.TYPE_CLASS_TEXT);
        _autoCompleteTextViewDestPhone.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View arg1, int index, long arg3) {

                String[] nameAndPhone = ((String) av.getItemAtPosition(index)).split("\\\n");
                String name = nameAndPhone[0];
                String number = nameAndPhone[1];
                String NumericNumber = PhoneNumberUtils.toValidPhoneNumber(number);

                _autoCompleteTextViewDestPhone.setText(NumericNumber);
                _destName = name;
                setDestNameTextView();
            }
        });

        _autoCompleteTextViewDestPhone.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                v.setFocusable(true);
                v.setFocusableInTouchMode(true);
                v.performClick();
                return false;
            }
        });

        _autoCompleteTextViewDestPhone.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                String destPhone = s.toString();

                if (10 == s.length() &&
                        PhoneNumberUtils.isNumeric(destPhone) &&
                        !wasFileChooser) {

                    _destPhoneNumber = destPhone;
                    drawSelectMediaButton(false);
                    drawRingToneName();

                    if (!getState().equals(AppStateManager.STATE_DISABLED) &&
                            !getState().equals(AppStateManager.STATE_LOADING)) {
                        String msg = "Fetching user data...";
                        BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG + " onTextchanged()",
                                new EventReport(EventType.FETCHING_USER_DATA, msg, null));

                        Intent i = new Intent(getApplicationContext(), LogicServerProxyService.class);
                        i.setAction(LogicServerProxyService.ACTION_ISREGISTERED);
                        i.putExtra(LogicServerProxyService.DESTINATION_ID, destPhone);
                        getApplicationContext().startService(i);

                        enableUserFetchProgressBar();
                    }
                } else {

                    // Resetting the flag
                    wasFileChooser = false;

                    _destPhoneNumber = "";
                    _destName = "";

                    if (10 != s.length() || !PhoneNumberUtils.isNumeric(destPhone)) {

                        disableUserStatusPositiveIcon();
                        vanishInviteButton();

                        if (getState().equals(AppStateManager.STATE_READY)) {
                            AppStateManager.setAppState(getApplicationContext(), TAG + " onTextChanged()", AppStateManager.STATE_IDLE);
                            BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG + "onTextChanged()", new EventReport(EventType.REFRESH_UI, "", null));
                        }
                    }
                }

                setDestNameTextView();
                saveInstanceState();

            }

            @Override
            public void afterTextChanged(Editable s) {


            }
        });

        new AutoCompletePopulateListAsyncTask(this, _autoCompleteTextViewDestPhone).execute();
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
        loginBtn.setText("Login");
    }

    private void prepareGetSmsCodeButton() {

        Button GetSMSCode = (Button) findViewById(R.id.GetSMSCode);
        GetSMSCode.setEnabled(false);

        OnClickListener buttonListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                EditText loginNumber = (EditText) findViewById(R.id.LoginNumber);

                //generate a 4 digit integer 1000 <10000
                _randomPIN = (int) (Math.random() * 9000) + 1000;
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(loginNumber.getText().toString(), null, "MediaCallz SmsVerificationCode: " + String.valueOf(_randomPIN), null, null);
                    Toast.makeText(getApplicationContext(), "Message Sent To: " + loginNumber.getText().toString(),
                            Toast.LENGTH_LONG).show();
                } catch (Exception ex) {
                    Toast.makeText(getApplicationContext(),
                            ex.getMessage(),
                            Toast.LENGTH_LONG).show();
                    ex.printStackTrace();
                }

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

    private void initializeConnection() {

        Intent i = new Intent();
        i.setClass(getBaseContext(), LogicServerProxyService.class);
        if (AppStateManager.getAppState(getApplicationContext()).equals(AppStateManager.STATE_LOGGED_OUT))
            i.setAction(LogicServerProxyService.ACTION_REGISTER);
        else
            i.setAction(LogicServerProxyService.ACTION_RECONNECT);

        startService(i);

    }

    /* -------------- UI methods -------------- */

    /* --- UI States --- */

    public void stateIdle() {

        disableProgressBar();
        enableSelectContactButton();
        enableContactEditText();
        disableUserFetchProgressBar();
        disableSelectProfileMediaButton();
        disableSelectCallerMediaButton();
        disableRingToneName();
        disableCallButton();

    }

    public void stateReady() {

        disableProgressBar();
        enableSelectMediaButton();
        drawRingToneName();
        disableUserFetchProgressBar();
        enableSelectProfileMediaButton();
        enableContactEditText();
        enableSelectContactButton();
        enableCallButton();
        userStatusRegistered();

    }

    public void stateDisabled() {

        disableSelectCallerMediaButton();
        disableSelectProfileMediaButton();
        disableUserFetchProgressBar();
        disableProgressBar();
        disableSelectContactButton();
        disableContactEditText();
        disableCallButton();
        disableInviteButton();

    }

    public void stateLoading() {

        enableProgressBar();
        disableSelectCallerMediaButton();
        disableSelectProfileMediaButton();
        disableSelectContactButton();
        disableContactEditText();
        disableCallButton();

    }

    private String getState() {

        return AppStateManager.getAppState(getApplicationContext());
    }

    private void syncUIwithAppState() {

        switch (AppStateManager.getAppState(getApplicationContext())) {
            case AppStateManager.STATE_LOGGED_OUT:
                initializeLoginUI();
                break;

            case AppStateManager.STATE_IDLE:
                stateIdle();
                break;

            case AppStateManager.STATE_READY:
                stateReady();
                break;

            case AppStateManager.STATE_LOADING:
                stateLoading();
                break;

            case AppStateManager.STATE_DISABLED:
                stateDisabled();
                break;
        }
    }

    private void enableHamburgerIconWithSlideMenu() {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);  //Enable or disable the "home" button in the corner of the action bar.
        }

        _mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        _DrawerList = (ListView) findViewById(R.id.left_drawer);
        addDrawerItems();
        _DrawerList.setOnItemClickListener(new DrawerItemClickListener());
        _mDrawerToggle = new ActionBarDrawerToggle(this, _mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //  getSupportActionBar().setTitle("Navigation!");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //   getSupportActionBar().setTitle(mActivityTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

        };
        _mDrawerToggle.setDrawerIndicatorEnabled(true);
        _mDrawerLayout.setDrawerListener(_mDrawerToggle);
        _mDrawerToggle.syncState();
    }

    /* --- UI elements controls --- */

    private void addDrawerItems() {

        // Add Drawer Item to dataList
        dataList.add(new DrawerItem("Media Management", R.drawable.mediaicon));
        dataList.add(new DrawerItem("Who Can MC me", R.drawable.blackwhitelist));
        dataList.add(new DrawerItem("How To ?", R.drawable.questionmark));
        dataList.add(new DrawerItem("Share Us", R.drawable.shareus));
        dataList.add(new DrawerItem("Rate Us", R.drawable.rateus2));
        dataList.add(new DrawerItem("Report Bug", R.drawable.bug));
        dataList.add(new DrawerItem("App Settings", R.drawable.settingsicon));

        mAdapter = new CustomDrawerAdapter(this, R.layout.custome_drawer_item,
                dataList);

        //   mAdapter = new ArrayAdapter<String>(this, R.layout.custome_drawer_item, osArray);
        _DrawerList.setAdapter(mAdapter);
    }

    public void selectNavigationItem(int position) {

        switch (position) {
            case 0://Media Management

                break;
            case 1: // Who Can MC me

                break;
            case 2: // How To?

                break;
            case 3: // Share Us
                break;
            case 4: // Rate Us
                shareUs();
                break;
            case 5: // Bug Report

                break;
            case 6: // App Settings
                appSettings();

                break;

            default:
                break;
        }


     /*   _DrawerList.setItemChecked(position, true);
        setTitle(dataList.get(position).getItemName());*/
        _mDrawerLayout.closeDrawer(_DrawerList);

    }

    private void shareUs() {

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "MediaCallz (Open it in Google Play Store to Download the Application)");

        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }

    private void appSettings() {
        saveInstanceState();
        Intent y = new Intent();
        y.setClass(getApplicationContext(), Settings.class);
        startActivity(y);
    }

    private void openCallerMediaMenu() {

        ImageButton callerMedia = (ImageButton) findViewById(R.id.selectMediaBtn);
        //Creating the instance of PopupMenu
        PopupMenu popup = new PopupMenu(MainActivity.this, callerMedia);
        //Inflating the Popup using xml file
        popup.getMenuInflater().inflate(R.menu.popup_menu_callermedia, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                Log.i(TAG, String.valueOf(item.getItemId()));
                switch (item.getItemId()) {
                    case R.id.selectcallermedia:
                        selectMedia(ActivityRequestCodes.SELECT_CALLER_MEDIA);
                        break;
                    case R.id.previewcallermedia:
                        //TODO Implement preview caller media
                        Toast.makeText(MainActivity.this, "CallerMenu Clicked : " + item.getTitle(), Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.clearcallermedia:
                        //TODO Mor: Disable this option in case no media has been uploaded
                        //TODO Mor: "Are you sure" dialog

                        clearMedia(SpecialMediaType.CALLER_MEDIA);
                        break;

                }
                return true;
            }
        });

        popup.show();

    }

    private void openProfileMediaMenu() {
        ImageButton profile = (ImageButton) findViewById(R.id.selectProfileMediaBtn);
        //Creating the instance of PopupMenu
        PopupMenu popup = new PopupMenu(MainActivity.this, profile);
        //Inflating the Popup using xml file
        popup.getMenuInflater().inflate(R.menu.popup_menu_profile, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {

                Log.i(TAG, String.valueOf(item.getItemId()));

                switch (item.getItemId()) {
                    case R.id.specificprofile:
                        selectMedia(ActivityRequestCodes.SELECT_PROFILE_MEDIA);
                        break;
                    case R.id.defaultprofile:
                        selectMedia(ActivityRequestCodes.SELECT_PROFILE_MEDIA);
                        break;
                    case R.id.previewprofilemedia:
                        Toast.makeText(MainActivity.this, "ProfileMenu Clicked : " + item.getTitle(), Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.clearprofilemedia:
                        //TODO Mor: Disable this option in case no media has been uploaded
                        //TODO Mor: "Are you sure" dialog

                        clearMedia(SpecialMediaType.PROFILE_MEDIA);
                        break;
                }
                return true;
            }
        });

        popup.show();
    }

    public void launchDialer(String number) {
        String numberToDial = "tel:" + number;
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(numberToDial)));
    }

    private void disableCallButton() {

        runOnUiThread(new Runnable() {

            public void run() {
                findViewById(R.id.CallNow).setVisibility(View.INVISIBLE);
            }

        });
    }

    private void enableCallButton() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                findViewById(R.id.CallNow).setVisibility(View.VISIBLE);
                findViewById(R.id.CallNow).setEnabled(true);
            }
        });
    }

    private void disableSelectCallerMediaButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.selectMediaBtn).setVisibility(View.INVISIBLE);
                findViewById(R.id.ringtoneName).setVisibility(View.INVISIBLE);
            }
        });
    }

    private void disableSelectProfileMediaButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.selectProfileMediaBtn).setClickable(false);
                drawSelectProfileMediaButton(false);
            }
        });
    }

    private void enableSelectMediaButton() {

        findViewById(R.id.selectMediaBtn).setClickable(true);
        drawSelectMediaButton(true);
        findViewById(R.id.selectMediaBtn).setVisibility(View.VISIBLE);
    }

    private void enableSelectProfileMediaButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.selectProfileMediaBtn).setClickable(true);
                drawSelectProfileMediaButton(true);
            }
        });
    }

    private void disableSelectContactButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.selectContactBtn).setEnabled(false);
                drawSelectContactButton(false);
            }
        });
    }

    private void enableSelectContactButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.selectContactBtn).setEnabled(true);
                drawSelectContactButton(true);
            }
        });
    }

    private void enableMediaStatusArrived() {

        ImageView mediaStatus = (ImageView) findViewById(R.id.mediaStatusArrived);
        mediaStatus.setVisibility(View.VISIBLE);
        mediaStatus.bringToFront();
    }

    private void disableMediaStatusArrived() {

        ImageView mediaStatus = (ImageView) findViewById(R.id.mediaStatusArrived);
        mediaStatus.setVisibility(View.INVISIBLE);
    }

    private void disableProgressBar() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _pBar = (ProgressBar) findViewById(R.id.progressBar);
                _pBar.setVisibility(ProgressBar.GONE);
            }
        });
    }

    private void enableProgressBar() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _pBar = (ProgressBar) findViewById(R.id.progressBar);
                if (_pBar != null)
                    _pBar.setVisibility(ProgressBar.VISIBLE);
            }
        });
    }

    private void disableContactEditText() {

        findViewById(R.id.CallNumber).setEnabled(false);
    }

    private void enableContactEditText() {

        findViewById(R.id.CallNumber).setEnabled(true);
    }

    private void disableUserFetchProgressBar() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _pFetchUserBar = (ProgressBar) findViewById(R.id.fetchuserprogress);
                _pFetchUserBar.setVisibility(ProgressBar.GONE);
            }
        });
    }

    private void enableUserFetchProgressBar() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _pFetchUserBar = (ProgressBar) findViewById(R.id.fetchuserprogress);
                _pFetchUserBar.setVisibility(ProgressBar.VISIBLE);

                disableUserStatusPositiveIcon();
                disableUserStatusNegativeIcon();
                vanishInviteButton();
            }
        });
    }

    private void userStatusRegistered() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                disableUserStatusNegativeIcon();
                enableUserStatusPositiveIcon();
                vanishInviteButton();
            }
        });
    }

    private void vanishInviteButton() {

        ImageButton invite = (ImageButton) findViewById(R.id.inviteButton);
        invite.setVisibility(View.INVISIBLE);
        invite.setClickable(false);
    }

    private void disableInviteButton() {

        ImageButton invite = (ImageButton) findViewById(R.id.inviteButton);
        invite.setEnabled(false);
    }

    private void enableInviteButton() {

        ImageButton invite = (ImageButton) findViewById(R.id.inviteButton);
        invite.setVisibility(View.VISIBLE);
        invite.setClickable(true);
        invite.bringToFront();
    }

    private void userStatusUnregistered() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                disableUserStatusPositiveIcon();
                enableUserStatusNegativeIcon();
                enableInviteButton();
            }
        });
    }

    private void disableUserStatusPositiveIcon() {

        ImageView userStatus = (ImageView) findViewById(R.id.userStatusPositive);
        userStatus.setVisibility(View.INVISIBLE);
    }

    private void disableUserStatusNegativeIcon() {

        ImageView userStatus = (ImageView) findViewById(R.id.userStatusNegative);
        userStatus.setVisibility(View.INVISIBLE);
    }

    private void enableUserStatusPositiveIcon() {

        ImageView userStatus = (ImageView) findViewById(R.id.userStatusPositive);
        userStatus.setVisibility(View.VISIBLE);
        userStatus.bringToFront();
    }

    private void enableUserStatusNegativeIcon() {

        ImageView userStatus = (ImageView) findViewById(R.id.userStatusNegative);
        userStatus.setVisibility(View.VISIBLE);
        userStatus.bringToFront();
    }

    private void disableRingToneStatusArrived() {

        ImageView ringtoneStatus = (ImageView) findViewById(R.id.ringtoneStatusArrived);
        ringtoneStatus.setVisibility(View.INVISIBLE);
    }

    private void enableRingToneStatusArrived() {

        ImageView ringtoneStatus = (ImageView) findViewById(R.id.ringtoneStatusArrived);
        ringtoneStatus.setVisibility(View.VISIBLE);
        ringtoneStatus.bringToFront();
    }

    private void drawSelectMediaButton(boolean enabled) {

        LUT_Utils lut_utils = new LUT_Utils(getApplicationContext(), SpecialMediaType.CALLER_MEDIA);
        try {
            FileManager.FileType fType;
            ImageButton selectCallerMediaBtn = (ImageButton) findViewById(R.id.selectMediaBtn);

            if (!enabled)
                selectCallerMediaBtn.setImageResource(R.drawable.defaultpic_disabled);
            else {

                String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(_destPhoneNumber);
                if (!lastUploadedMediaPath.equals("")) {
                    fType = FileManager.getFileType(lastUploadedMediaPath);

                    BitmapUtils.execBitMapWorkerTask(selectCallerMediaBtn, fType, lastUploadedMediaPath, false);

                    enableMediaStatusArrived();

                } else {// enabled but no uploaded media
                    if (enabled)
                        selectCallerMediaBtn.setImageResource(R.drawable.defaultpic_enabled);
                    else
                        selectCallerMediaBtn.setImageResource(R.drawable.defaultpic_disabled);

                    disableMediaStatusArrived();
                }
            }

        } catch (FileInvalidFormatException |
                FileDoesNotExistException |
                FileMissingExtensionException e) {
            e.printStackTrace();
            lut_utils.removeUploadedMediaPerNumber(_destPhoneNumber);
        } finally {
            lut_utils.destroy();
        }
    }

    private void drawSelectProfileMediaButton(boolean enabled) {

        LUT_Utils lut_utils = new LUT_Utils(getApplicationContext(), SpecialMediaType.PROFILE_MEDIA);

        try {

            ImageButton selectProfileMediaBtn = (ImageButton) findViewById(R.id.selectProfileMediaBtn);

            if (!enabled) {
                BitmapUtils.execBitmapWorkerTask(selectProfileMediaBtn, getApplicationContext(), getResources(), R.drawable.defaultpic_disabled, true);
            } else {

                String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(_destPhoneNumber);
                if (!lastUploadedMediaPath.equals("")) {
                    FileManager.FileType fType = FileManager.getFileType(lastUploadedMediaPath);

                    BitmapUtils.execBitMapWorkerTask(selectProfileMediaBtn, fType, lastUploadedMediaPath, true);
                } else // enabled but no uploaded media

                    BitmapUtils.execBitmapWorkerTask(selectProfileMediaBtn, getApplicationContext(), getResources(), R.drawable.defaultpic_enabled, true);
            }

        } catch (FileInvalidFormatException |
                FileDoesNotExistException |
                FileMissingExtensionException e) {
            e.printStackTrace();
            lut_utils.removeUploadedMediaPerNumber(_destPhoneNumber);
        } finally {
            lut_utils.destroy();
        }
    }

    private void drawRingToneName() {

        LUT_Utils lut_utils = new LUT_Utils(getApplicationContext(), SpecialMediaType.CALLER_MEDIA);
        String ringToneFilePath = lut_utils.getUploadedTonePerNumber(_destPhoneNumber);
        TextView ringtoneView = (TextView) findViewById(R.id.ringtoneName);

        try {

            if (!ringToneFilePath.isEmpty()) {
                ringtoneView.setText(FileManager.getFileNameWithExtension(ringToneFilePath));
                ringtoneView.setBackgroundColor(0xFF00FF00);
                ringtoneView.setVisibility(View.VISIBLE);

                enableRingToneStatusArrived();

            } else {
                ringtoneView.setVisibility(View.INVISIBLE);

                disableRingToneStatusArrived();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to draw drawRingToneName:" + (e.getMessage() != null ? e.getMessage() : e));
        } finally {
            lut_utils.destroy();
        }
    }

    private void disableRingToneName() {

        TextView ringtoneView = (TextView) findViewById(R.id.ringtoneName);
        ringtoneView.setVisibility(View.INVISIBLE);
        disableRingToneStatusArrived();
    }

    private void drawSelectContactButton(boolean enabled) {

        ImageButton selectContactButton = (ImageButton) findViewById(R.id.selectContactBtn);
        if (enabled)
            selectContactButton.setImageResource(R.drawable.select_contact_enabled);
        else
            selectContactButton.setImageResource(R.drawable.select_contact_disabled);
    }

    private void writeErrStatBar(final String text) {

    }

    private void writeInfoSnackBar(final String text) {

        Log.i(TAG, "Snackbar showing:" + text);

        SnackbarManager.show(
                Snackbar.with(getApplicationContext()) // context
                        .text(text) // text to display
                        .actionLabel("Close") // action button label
                        .duration(Snackbar.SnackbarDuration.LENGTH_LONG)
                        .actionListener(new ActionClickListener() {
                            @Override
                            public void onActionClicked(Snackbar snackbar) {
                                SnackbarManager.dismiss();
                            }
                        }) // action button's ActionClickListener
                , this); // activity where it is displayed

    }

    private void writeInfoSnackBar(final String text, final int color, Snackbar.SnackbarDuration duration) {

        Log.i(TAG, "Snackbar showing:" + text);

        SnackbarManager.show(
                Snackbar.with(getApplicationContext()) // context
                        .text(text) // text to display
                        .textColor(color)
                        .actionLabel("Close") // action button label
                        .duration(duration)
                        .actionListener(new ActionClickListener() {
                            @Override
                            public void onActionClicked(Snackbar snackbar) {
                                SnackbarManager.dismiss();
                            }
                        }) // action button's ActionClickListener
                , this); // activity where it is displayed
    }

    private void handleSnackBar(SnackbarData snackbarData) {

        switch (snackbarData.getStatus()) {
            case CLOSE:
                SnackbarManager.dismiss();
                break;

            case SHOW:
                writeInfoSnackBar(snackbarData.getText(), snackbarData.getColor(), snackbarData.getmDuration());
                break;
        }
    }

    private abstract class ActivityRequestCodes {

        public static final int SELECT_CALLER_MEDIA = 1;
        public static final int SELECT_CONTACT = 2;
        public static final int SELECT_PROFILE_MEDIA = 3;
        public static final int SELECT_MEDIA = 4;

    }

    private class DrawerItemClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            if (position != 0)
                selectNavigationItem(position);

        }
    }
}