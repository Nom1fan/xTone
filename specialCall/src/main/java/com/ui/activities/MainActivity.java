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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.app.AppStateManager;
import com.async_tasks.AutoCompletePopulateListAsyncTask;
import com.async_tasks.IsRegisteredTask;
import com.batch.android.Batch;
import com.data_objects.ActivityRequestCodes;
import com.data_objects.Contact;
import com.data_objects.SnackbarData;
import com.interfaces.ICallbackListener;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.services.IncomingService;
import com.services.LogicServerProxyService;
import com.services.OutgoingService;
import com.services.StorageServerProxyService;
import com.special.app.R;
import com.utils.BitmapUtils;
import com.utils.ContactsUtils;
import com.utils.LUT_Utils;
import com.utils.PhoneNumberUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import DataObjects.SpecialMediaType;
import EventObjects.Event;
import EventObjects.EventReport;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;

public class MainActivity extends AppCompatActivity implements OnClickListener, ICallbackListener {

    private final String TAG = MainActivity.class.getSimpleName();
    private String _destPhoneNumber = "";
    private String _destName = "";
    private ProgressBar _pFetchUserBar;
    private ProgressBar _pBar;
    private BroadcastReceiver _eventReceiver;
    private IntentFilter _eventIntentFilter = new IntentFilter(Event.EVENT_ACTION);
    private AutoCompleteTextView _autoCompleteTextViewDestPhone;
    private ListView _DrawerList;
    private ActionBarDrawerToggle _mDrawerToggle;
    private DrawerLayout _mDrawerLayout;

    //region Activity methods (onCreate(), onPause(), ...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        if(getState().equals(AppStateManager.STATE_LOGGED_OUT)) {

            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
            finish();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (_mDrawerLayout != null)
            _mDrawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart()");

        Batch.onStart(this);

        prepareEventReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");

        String appState = getState();
        Log.i(TAG, "App State:" + appState);

        if(!getState().equals(AppStateManager.STATE_LOGGED_OUT)) {
            //TODO MediaCallz: Do we need these start services here?
            // Starting service responsible for incoming media callz
            Intent incomingServiceIntent = new Intent(this, IncomingService.class);
            incomingServiceIntent.setAction(IncomingService.ACTION_START);
            startService(incomingServiceIntent);

            // Starting service responsible for outgoing media callz
            Intent outgoingServiceIntent = new Intent(this, OutgoingService.class);
            outgoingServiceIntent.setAction(OutgoingService.ACTION_START);
            startService(outgoingServiceIntent);

            initializeUI();
            syncUIwithAppState();

            // Taking Focus from AutoCompleteTextView in the end, so he won't pop up :) added also focus capabilities to the MainActivity Layout XML
            findViewById(R.id.mainActivity).requestFocus();

            prepareEventReceiver();

            // ASYNC TASK To Populate all contacts , it can take long time and it delays the UI
            new AutoCompletePopulateListAsyncTask(_autoCompleteTextViewDestPhone).execute(getApplicationContext());

            if (appState.equals(AppStateManager.STATE_DISABLED))
                reconnect();

            restoreInstanceState();
        }

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

        UI_Utils.unbindDrawables(findViewById(R.id.mainActivity));
        System.gc();
    }

    @Override
    protected void onStop() {
        Batch.onStop(this);

        super.onStop();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == ActivityRequestCodes.SELECT_MEDIA && data!=null) {
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
    public boolean onKeyDown(int keyCode, KeyEvent e) {  // hard menu key will open and close the drawer menu also
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
    //endregion (on

    //region Assisting methods (onClick(), eventReceived(), ...)
    private void selectMedia(int specialMediaType) {

        /* Create an intent that will start the main activity. */
        Intent mainIntent = new Intent(MainActivity.this,
                SelectMediaActivity.class);
        mainIntent.putExtra("SpecialMediaType", specialMediaType);
        mainIntent.putExtra("DestinationNumber", _destPhoneNumber);
        mainIntent.putExtra("DestinationName", _destName);
        startActivityForResult(mainIntent, ActivityRequestCodes.SELECT_MEDIA);

         /* Apply our splash exit (fade out) and main
            entry (fade in) animation transitions. */
        overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation); // open drawer animation
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
                smsManager.sendTextMessage(callNumber.getText().toString(), null, getResources().getString(R.string.invite), null, null);
                writeInfoSnackBar( getResources().getString(R.string.sent_invitation) + callNumber.getText().toString());

            } catch (Exception ex) {
                writeErrStatBar(ex.getMessage());
            }
        }
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
                    handleSnackBar(data);
                break;

            case DISPLAY_ERROR:
                break;

            case DISPLAY_MESSAGE:
                handleSnackBar(new SnackbarData(
                        SnackbarData.SnackbarStatus.SHOW,
                        Color.GREEN,
                        Snackbar.SnackbarDuration.LENGTH_LONG,
                        report.desc()));
                break;

            default: // Event not meant for MainActivity receiver
        }
    }

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
    }

    private void setDestNameTextView() {

        final TextView tv_destName =
                (TextView) findViewById(R.id.destName);
        if (tv_destName != null && _destName != null)
            tv_destName.setText(_destName);
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

    private void prepareAutoCompleteTextViewDestPhoneNumber() {

        final MainActivity instance = this;

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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                String destPhone = s.toString();

                if (PhoneNumberUtils.isValidPhoneNumber(destPhone)) {

                    _destPhoneNumber = destPhone;
                    new IsRegisteredTask(destPhone, instance).execute(instance.getApplicationContext());

                } else { // Invalid destination number

                    _destPhoneNumber = "";
                    _destName = "";

                    disableUserStatusPositiveIcon();
                    disableUserStatusNegativeIcon();
                    vanishInviteButton();

                    if (getState().equals(AppStateManager.STATE_READY)) {
                        AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_IDLE);
                        syncUIwithAppState();
                    }
                }

                setDestNameTextView();
                saveInstanceState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        new AutoCompletePopulateListAsyncTask(_autoCompleteTextViewDestPhone).execute(getApplicationContext());
    }

    private void reconnect() {

        Intent i = new Intent();
        i.setClass(getBaseContext(), LogicServerProxyService.class);
        i.setAction(LogicServerProxyService.ACTION_RECONNECT);
        startService(i);
    }

    private void BlockMCContacts() {
        saveInstanceState();
        Intent y = new Intent();
        y.setClass(getApplicationContext(), BlockMCContacts.class);
        startActivity(y);
    }
    //endregion

    //region UI methods
    //region UI initializers
    private void initializeUI() {

        setContentView(R.layout.activity_main);

        setCustomActionBar();
        enableHamburgerIconWithSlideMenu();
        prepareAutoCompleteTextViewDestPhoneNumber();

        findViewById(R.id.CallNow).setOnClickListener(this);
        findViewById(R.id.selectMediaBtn).setOnClickListener(this);
        findViewById(R.id.selectContactBtn).setOnClickListener(this);
        findViewById(R.id.selectProfileMediaBtn).setOnClickListener(this);
        findViewById(R.id.clear).setOnClickListener(this);
        findViewById(R.id.inviteButton).setOnClickListener(this);
    }

    //endregion

    //region UI States
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
    //endregion

    //region UI elements controls
    private void setCustomActionBar() {

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.custom_action_bar);
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

    private void addDrawerItems() {

        // Add Drawer Item to dataList
        List<DrawerItem> dataList = new ArrayList<>();
        dataList.add(new DrawerItem(getResources().getString(R.string.media_management), R.drawable.mediaicon));
        dataList.add(new DrawerItem(getResources().getString(R.string.who_can_mc_me), R.drawable.blackwhitelist));
        dataList.add(new DrawerItem(getResources().getString(R.string.how_to), R.drawable.questionmark));
        dataList.add(new DrawerItem(getResources().getString(R.string.share_us), R.drawable.shareus));
        dataList.add(new DrawerItem(getResources().getString(R.string.rate_us), R.drawable.rateus2));
        dataList.add(new DrawerItem(getResources().getString(R.string.report_bug), R.drawable.bug));
        dataList.add(new DrawerItem(getResources().getString(R.string.app_settings), R.drawable.settingsicon));

        CustomDrawerAdapter mAdapter = new CustomDrawerAdapter(this, R.layout.custome_drawer_item,
                dataList);

        //   mAdapter = new ArrayAdapter<String>(this, R.layout.custome_drawer_item, osArray);
        _DrawerList.setAdapter(mAdapter);
    }

    public void selectNavigationItem(int position) {

        switch (position) {
            case 0://Media Management
                appSettings();
                break;
            case 1: // Who Can MC me
                BlockMCContacts();
                break;
            case 2: // How To?
                //TODO IMPLEMET Case
                break;
            case 3: // Share Us
                shareUs();
                break;
            case 4: // Rate Us
                //TODO IMPLEMET Case
                break;
            case 5: // Report BUG
                //TODO IMPLEMET Case
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
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_subject));

        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, getResources().getString(R.string.invite));
        startActivity(Intent.createChooser(sharingIntent,getResources().getString(R.string.share_via)));
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

        _pFetchUserBar = (ProgressBar) findViewById(R.id.fetchuserprogress);
        _pFetchUserBar.setVisibility(ProgressBar.GONE);

    }

    private void enableUserFetchProgressBar() {

        _pFetchUserBar = (ProgressBar) findViewById(R.id.fetchuserprogress);
        _pFetchUserBar.setVisibility(ProgressBar.VISIBLE);

        disableUserStatusPositiveIcon();
        disableUserStatusNegativeIcon();
        vanishInviteButton();
    }

    private void userStatusRegistered() {

        disableUserStatusNegativeIcon();
        enableUserStatusPositiveIcon();
        vanishInviteButton();

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

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.CALLER_MEDIA);
        try {
            FileManager.FileType fType;
            ImageButton selectCallerMediaBtn = (ImageButton) findViewById(R.id.selectMediaBtn);

            if (!enabled)
                selectCallerMediaBtn.setImageResource(R.drawable.defaultpic_disabled);
            else {

                String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(getApplicationContext(), _destPhoneNumber);
                if (!lastUploadedMediaPath.equals("")) {
                    fType = FileManager.getFileType(lastUploadedMediaPath);

                    BitmapUtils.execBitMapWorkerTask(selectCallerMediaBtn, fType, lastUploadedMediaPath, false);

                    enableMediaStatusArrived();

                } else {// enabled but no uploaded media
                    selectCallerMediaBtn.setImageResource(R.drawable.defaultpic_enabled);

                    disableMediaStatusArrived();
                }
            }

        } catch (FileInvalidFormatException |
                FileDoesNotExistException |
                FileMissingExtensionException e) {
            e.printStackTrace();
            lut_utils.removeUploadedMediaPerNumber(getApplicationContext(), _destPhoneNumber);
        }
    }

    private void drawSelectProfileMediaButton(boolean enabled) {

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.PROFILE_MEDIA);

        try {

            ImageButton selectProfileMediaBtn = (ImageButton) findViewById(R.id.selectProfileMediaBtn);

            if (!enabled) {
                BitmapUtils.execBitmapWorkerTask(selectProfileMediaBtn, getApplicationContext(), getResources(), R.drawable.defaultpic_disabled, true);
            } else {

                String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(getApplicationContext(), _destPhoneNumber);
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
            lut_utils.removeUploadedMediaPerNumber(getApplicationContext(), _destPhoneNumber);
        }
    }

    private void drawRingToneName() {

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.CALLER_MEDIA);
        String ringToneFilePath = lut_utils.getUploadedTonePerNumber(getApplicationContext(), _destPhoneNumber);
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
                        .actionLabel(getResources().getString(R.string.snack_close)) // action button label
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
                        .actionLabel(getResources().getString(R.string.snack_close)) // action button label
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
                if(snackbarData.getText()!=null && !snackbarData.getText().equals(""))
                    writeInfoSnackBar(snackbarData.getText(), snackbarData.getColor(), snackbarData.getmDuration());
                break;
        }
    }
    //endregion
    //endregion

    //region ICallbackListener methods
    @Override
    public void doCallBackAction() {

    }

    @Override
    public void doCallBackAction(final Object... params) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                String[] sParams = Arrays.copyOf(params, params.length, String[].class);
                for (int i = 0; i < params.length; ++i) {

                    switch (sParams[i]) {

                        case IsRegisteredTask.DRAW_SELECT_MEDIA_FALSE:
                            drawSelectProfileMediaButton(false);
                            break;

                        case IsRegisteredTask.ENABLE_FETCH_PROGRESS_BAR:
                            enableUserFetchProgressBar();
                            break;
                    }
                }
            }
        });

    }
    //endregion

    //region private classes
    private class DrawerItemClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            if (position != 0)
                selectNavigationItem(position);
        }
    }
    //endregion

      /*  @Override     //  the menu with the 3 dots on the right, on the top action bar, to enable it uncomment this.
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }*/
}