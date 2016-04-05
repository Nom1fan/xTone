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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.app.AppStateManager;
import com.async_tasks.AutoCompletePopulateListAsyncTask;
import com.async_tasks.IsRegisteredTask;
import com.batch.android.Batch;
import com.data_objects.ActivityRequestCodes;
import com.data_objects.Constants;
import com.data_objects.Contact;
import com.data_objects.SnackbarData;
import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.interfaces.ICallbackListener;
import com.mediacallz.app.R;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.services.AbstractStandOutService;
import com.services.IncomingService;
import com.services.LogicServerProxyService;
import com.services.OutgoingService;
import com.services.StorageServerProxyService;
import com.ui.dialogs.MandatoryUpdateDialog;
import com.utils.BitmapUtils;
import com.utils.ContactsUtils;
import com.utils.LUT_Utils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import DataObjects.AppMetaRecord;
import DataObjects.SharedConstants;
import DataObjects.SpecialMediaType;
import EventObjects.Event;
import EventObjects.EventReport;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;
import utils.PhoneNumberUtils;

public class MainActivity extends AppCompatActivity implements OnClickListener, ICallbackListener {

    private final String TAG = MainActivity.class.getSimpleName();
    private final String shareBody = String.valueOf(R.string.invite);

    private String _destPhoneNumber = "";
    private String _destName = "";

    //region UI elements
    private ImageView _userStatusPositive;
    private ImageView _userStatusNegative;
    private ImageButton _selectContactBtn;
    private ImageButton _selectMediaBtn;
    private ImageButton _callBtn;
    private ProgressBar _fetchUserPbar;
    private ProgressBar _pBar;
    private BroadcastReceiver _eventReceiver;
    private IntentFilter _eventIntentFilter = new IntentFilter(Event.EVENT_ACTION);
    private AutoCompleteTextView _autoCompleteTextViewDestPhone;
    private ListView _DrawerList;
    private ActionBarDrawerToggle _mDrawerToggle;
    private DrawerLayout _mDrawerLayout;
    private ImageView _mediaStatus;
    private ImageButton _defaultpic_enabled;
    private TextView _ringToneNameTextView;
    private ImageButton _inviteBtn;
    private RelativeLayout _mainActivityLayout;
    private ImageView _ringtoneStatus;
    private AutoCompleteTextView _destinationEditText;
    //endregion

    //region Activity methods (onCreate(), onPause(), ...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        startLoginActivityIfLoggedOut();

        initializeUI();
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

        startLoginActivityIfLoggedOut();

        prepareEventReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");

        String appState = getState();
        Log.i(TAG, "App State:" + appState);

        if (!getState().equals(AppStateManager.STATE_LOGGED_OUT)) {
            //TODO MediaCallz: Do we need these start services here?
            // Starting service responsible for incoming media callz
            Intent incomingServiceIntent = new Intent(this, IncomingService.class);
            incomingServiceIntent.setAction(IncomingService.ACTION_START);
            startService(incomingServiceIntent);

            // Starting service responsible for outgoing media callz
            Intent outgoingServiceIntent = new Intent(this, OutgoingService.class);
            outgoingServiceIntent.setAction(OutgoingService.ACTION_START);
            startService(outgoingServiceIntent);

            syncUIwithAppState();

            // Taking Focus from AutoCompleteTextView in the end, so he won't pop up :) added also focus capabilities to the MainActivity Layout XML
            _mainActivityLayout.requestFocus();

            prepareEventReceiver();

            // ASYNC TASK To Populate all contacts , it can take long time and it delays the UI
            new AutoCompletePopulateListAsyncTask(_autoCompleteTextViewDestPhone).execute(getApplicationContext());

            if (appState.equals(AppStateManager.STATE_DISABLED))
                reconnect();

            restoreInstanceState();

            getAppRecord();

            showCaseViewCallNumber();
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

//        UI_Utils.unbindDrawables(findViewById(R.id.mainActivity));
//        System.gc();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop()");
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

            if (requestCode == ActivityRequestCodes.SELECT_MEDIA) {
                if (data != null)
                    writeInfoSnackBar(data.getStringExtra(SelectMediaActivity.RESULT_MSG), Color.RED, Snackbar.SnackbarDuration.LENGTH_INDEFINITE);
            }

            if (requestCode == ActivityRequestCodes.SELECT_CONTACT) {
                try {
                    if (data != null) {
                        Uri uri = data.getData();
                        Contact contact = ContactsUtils.getContact(uri, getApplicationContext());
                        saveInstanceState(contact.get_name(), PhoneNumberUtils.toValidLocalPhoneNumber(contact.get_phoneNumber()));
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
    private void startLoginActivityIfLoggedOut() {

        if (getState().equals(AppStateManager.STATE_LOGGED_OUT)) {

            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
            finish();
        }
    }

    private void startPreviewStandoutWindow(SpecialMediaType specialMediaType) {

        // close previous
        Intent closePrevious = new Intent(getApplicationContext(), OutgoingService.class);
        closePrevious.setAction(AbstractStandOutService.ACTION_CLOSE_ALL);
        startService(closePrevious);

        LUT_Utils lut_utils = new LUT_Utils(specialMediaType);
        Intent showPreview = new Intent(getApplicationContext(), OutgoingService.class);
        showPreview.setAction(AbstractStandOutService.ACTION_PREVIEW);

        showPreview.putExtra(AbstractStandOutService.PREVIEW_AUDIO, lut_utils.getUploadedTonePerNumber(getApplicationContext(), _destPhoneNumber));
        showPreview.putExtra(AbstractStandOutService.PREVIEW_VISUAL_MEDIA, lut_utils.getUploadedMediaPerNumber(getApplicationContext(), _destPhoneNumber));


        startService(showPreview);



    }

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
                smsManager.sendTextMessage(callNumber.getText().toString(), null, shareBody, null, null);
                writeInfoSnackBar("Invitation Sent To: " + callNumber.getText().toString());

            } catch (Exception ex) {
                writeErrStatBar(ex.getMessage());
            }
        }
    }

    public void eventReceived(Event event) {

        final EventReport report = event.report();

        switch (report.status()) {

            case APP_RECORD_RECEIVED:
                AppMetaRecord appMetaRecord = (AppMetaRecord)report.data();

                if(SharedConstants.APP_VERSION < appMetaRecord.get_lastSupportedVersion())
                    showMandatoryUpdateDialog(appMetaRecord.get_appVersion());
                break;

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
        if (ed_destinationName != null && (!ed_destinationName.getText().toString().isEmpty())) {
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
        final TextView tv_destNameTitle =
                (TextView) findViewById(R.id.media_calling);
        if (tv_destName != null && _destName != null)
        {
            tv_destName.setText(_destName);

            if (!_destName.isEmpty())
                tv_destNameTitle.setVisibility(View.VISIBLE);
            else
                tv_destNameTitle.setVisibility(View.INVISIBLE);
        }
        else
            tv_destNameTitle.setVisibility(View.INVISIBLE);
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
        if (_autoCompleteTextViewDestPhone != null) {
            _autoCompleteTextViewDestPhone.setRawInputType(InputType.TYPE_CLASS_TEXT);
            _autoCompleteTextViewDestPhone.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> av, View arg1, int index, long arg3) {

                    String[] nameAndPhone = ((String) av.getItemAtPosition(index)).split("\\\n");
                    String name = nameAndPhone[0];
                    String number = nameAndPhone[1];
                    String NumericNumber = PhoneNumberUtils.toValidLocalPhoneNumber(number);

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

                        if (destPhone.equals(Constants.MY_ID(getApplicationContext()))) {
                            UI_Utils.showSnackBar(getResources().getString(R.string.cant_send_to_self),
                                    Color.YELLOW, Snackbar.SnackbarDuration.LENGTH_LONG, getApplicationContext());
                        } else {
                            _destPhoneNumber = destPhone;
                            new IsRegisteredTask(destPhone, instance).execute(instance.getApplicationContext());
                        }

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

        prepareMainActivityLayout();

        setCustomActionBar();
        enableHamburgerIconWithSlideMenu();
        prepareAutoCompleteTextViewDestPhoneNumber();

        prepareUserStatusNegative();
        prepareUserStatusPositive();
        prepareContactEditText();
        prepareRingtoneStatus();
        prepareProgressBar();
        prepareFetchUserProgressBar();
        prepareRingtoneNameTextView();
        prepareMediaStatusImageView();
        prepareCallNowButton();
        prepareSelectMediaButton();
        prepareSelectContactButton();
        prepareSelectProfileMediaButton();
        prepareClearTextButton();
        prepareInviteButton();
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
    private void prepareUserStatusPositive() {

        _userStatusPositive = (ImageView) findViewById(R.id.userStatusPositive);
    }

    private void prepareUserStatusNegative() {

        _userStatusNegative = (ImageView) findViewById(R.id.userStatusNegative);
    }

    private void prepareContactEditText() {

        _destinationEditText = (AutoCompleteTextView) findViewById(R.id.CallNumber);
    }

    private void prepareRingtoneStatus() {

        _ringtoneStatus = (ImageView) findViewById(R.id.ringtoneStatusArrived);
    }

    private void prepareProgressBar() {

        _pBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    private void prepareMainActivityLayout() {

        _mainActivityLayout = (RelativeLayout) findViewById(R.id.mainActivity);
    }

    private void prepareFetchUserProgressBar() {

        _fetchUserPbar = (ProgressBar) findViewById(R.id.fetchuserprogress);
    }

    private void prepareMediaStatusImageView() {

        _mediaStatus = (ImageView) findViewById(R.id.mediaStatusArrived);
    }

    private void prepareRingtoneNameTextView() {

        _ringToneNameTextView = (TextView) findViewById(R.id.ringtoneName);
    }

    private void prepareInviteButton() {

        _inviteBtn = (ImageButton) findViewById(R.id.inviteButton);
        if (_inviteBtn != null)
            _inviteBtn.setOnClickListener(this);
    }

    private void prepareClearTextButton() {

        ImageButton clearText = (ImageButton) findViewById(R.id.clear);
        if (clearText != null)
            clearText.setOnClickListener(this);
    }

    private void prepareCallNowButton() {

        _callBtn = (ImageButton) findViewById(R.id.CallNow);
        if (_callBtn != null)
            _callBtn.setOnClickListener(this);
    }

    private void prepareSelectMediaButton() {

        _selectMediaBtn = (ImageButton) findViewById(R.id.selectMediaBtn);
        if (_selectMediaBtn != null)
            _selectMediaBtn.setOnClickListener(this);
    }

    private void prepareSelectContactButton() {

        _selectContactBtn = (ImageButton) findViewById(R.id.selectContactBtn);
        if (_selectContactBtn != null) {
            _selectContactBtn.setOnClickListener(this);
        }

    }

    private void prepareSelectProfileMediaButton() {

        _defaultpic_enabled = (ImageButton) findViewById(R.id.selectProfileMediaBtn);
        if (_defaultpic_enabled != null)
            _defaultpic_enabled.setOnClickListener(this);
    }

    private void setCustomActionBar() {

        ActionBar _actionBar = getSupportActionBar();
        if (_actionBar != null) {
            _actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            _actionBar.setCustomView(R.layout.custom_action_bar);
        }
    }

    private void enableHamburgerIconWithSlideMenu() {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);  //Enable or disable the "home" button in the corner of the action bar.
        }

        _mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (_mDrawerLayout != null) {
            _DrawerList = (ListView) findViewById(R.id.left_drawer);
            addDrawerItems();
            _DrawerList.setOnItemClickListener(new DrawerItemClickListener());
            _mDrawerToggle = new ActionBarDrawerToggle(this, _mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

                /**
                 * Called when a drawer has settled in a completely open state.
                 */
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                    //  getSupportActionBar().setTitle("Navigation!");
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

                /**
                 * Called when a drawer has settled in a completely closed state.
                 */
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
    }

    private void addDrawerItems() {

        // Add Drawer Item to dataList
        List<DrawerItem> dataList = new ArrayList<>();
        dataList.add(new DrawerItem("Media Management", R.drawable.mediaicon));
        dataList.add(new DrawerItem("Who Can MC me", R.drawable.blackwhitelist));
        dataList.add(new DrawerItem("How To ?", R.drawable.questionmark));
        dataList.add(new DrawerItem("Share Us", R.drawable.shareus));
        dataList.add(new DrawerItem("Rate Us", R.drawable.rateus2));
        dataList.add(new DrawerItem("Report Bug", R.drawable.bug));
        dataList.add(new DrawerItem("App Settings", R.drawable.settingsicon));

        CustomDrawerAdapter mAdapter = new CustomDrawerAdapter(this, R.layout.custome_drawer_item,
                dataList);

        //   mAdapter = new ArrayAdapter<String>(this, R.layout.custome_drawer_item, osArray);
        _DrawerList.setAdapter(mAdapter);
    }

    private void selectNavigationItem(int position) {

        switch (position) {
            case 0://Media Management
                appSettings();
                break;
            case 1: // Who Can MC me
                BlockMCContacts();
                break;
            case 2: // How To?
                resetShowcases();
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

    private void resetShowcases() {

        _mDrawerLayout.closeDrawer(GravityCompat.START);
        AutoCompleteTextView textViewToClear = (AutoCompleteTextView) findViewById(R.id.CallNumber);
        textViewToClear.setText("");


        SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW, false);
        SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.SELECT_MEDIA_VIEW, false);
        SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.UPLOAD_BEFORE_CALL_VIEW, false);

        showCaseViewCallNumber();
    }

    private void showCaseViewCallNumber() {
        if (!(SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW))) {
            ViewTarget targetCallNumber = new ViewTarget(R.id.selectContactBtn, MainActivity.this);
            UI_Utils.showCaseView(MainActivity.this, targetCallNumber, getResources().getString(R.string.callnumber_sv_title), getResources().getString(R.string.callnumber_sv_details));
            SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW, true);
        }
    }

    private void showCaseViewSelectMedia(){
        if (!(SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.SELECT_MEDIA_VIEW))) {
            ViewTarget targetSelectMediaView = new ViewTarget(R.id.selectMediaBtn, this);
            ShowcaseView sv = new ShowcaseView.Builder(MainActivity.this)
                    .setTarget(targetSelectMediaView)
                    .setContentTitle(getResources().getString(R.string.callermedia_sv_title))
                    .setContentText(getResources().getString(R.string.callermedia_sv_details))
                    .hideOnTouchOutside().
                            withMaterialShowcase().build();

            sv.setOnShowcaseEventListener(new OnShowcaseEventListener() {
                @Override
                public void onShowcaseViewHide(ShowcaseView showcaseView) {

                    showCaseViewSelectProfile();
                }

                @Override
                public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                }

                @Override
                public void onShowcaseViewShow(ShowcaseView showcaseView) {
                }

                @Override
                public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {
                }
            });

            SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.SELECT_MEDIA_VIEW, true);
        }
    }

    private void showCaseViewSelectProfile() {

        ViewTarget targetProfileView = new ViewTarget(R.id.selectProfileMediaBtn, this);
        UI_Utils.showCaseView(MainActivity.this, targetProfileView, getResources().getString(R.string.profile_sv_title), getResources().getString(R.string.profile_sv_details));
}

    private void showCaseViewCall() {

        ViewTarget targetCallView = new ViewTarget(R.id.CallNow, this);
        UI_Utils.showCaseView(MainActivity.this, targetCallView, getResources().getString(R.string.call_sv_title), getResources().getString(R.string.call_sv_details));
    }

    private void showCaseViewAfterUploadAndCall() {

      if (!(SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.UPLOAD_BEFORE_CALL_VIEW)))
        {
        ViewTarget targetSelectMediaView = new ViewTarget(R.id.selectMediaBtn, this);
        ShowcaseView sv = new ShowcaseView.Builder(MainActivity.this)
                .setTarget(targetSelectMediaView)
                .setContentTitle(getResources().getString(R.string.callermedia_sv_title))
                .setContentText(getResources().getString(R.string.callermedia_sv_details_image_ringtone))
                .hideOnTouchOutside().
                        withMaterialShowcase().build();

        sv.setOnShowcaseEventListener(new OnShowcaseEventListener() {
            @Override
            public void onShowcaseViewHide(ShowcaseView showcaseView) {

                showCaseViewCall();
            }

            @Override
            public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

            }

            @Override
            public void onShowcaseViewShow(ShowcaseView showcaseView) {

            }

            @Override
            public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {

            }
        });
        SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.UPLOAD_BEFORE_CALL_VIEW, true);
    }
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
//        Intent y = new Intent();
//        y.setClass(getApplicationContext(), Settings.class);
//        startActivity(y);
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, SetSettingsActivity.class);
        startActivity(intent);
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

                        startPreviewStandoutWindow(SpecialMediaType.CALLER_MEDIA);

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
                    case R.id.previewprofilemedia:
                        startPreviewStandoutWindow(SpecialMediaType.PROFILE_MEDIA);
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

        _callBtn.setVisibility(View.INVISIBLE);
    }

    private void enableCallButton() {

        _callBtn.setVisibility(View.VISIBLE);
        _callBtn.setEnabled(true);
    }

    private void disableSelectCallerMediaButton() {

        _selectMediaBtn.setVisibility(View.INVISIBLE);
        _ringToneNameTextView.setVisibility(View.INVISIBLE);
    }

    private void disableSelectProfileMediaButton() {

        _defaultpic_enabled.setClickable(false);
        drawSelectProfileMediaButton(false);
    }

    private void enableSelectMediaButton() {

        _selectMediaBtn.setClickable(true);

        // make the drawable fit center and with padding, so it won't stretch
        int sizeInDp = 30;
        float scale = getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (sizeInDp*scale + 0.5f);
        _selectMediaBtn.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels);
        _selectMediaBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);

        drawSelectMediaButton(true);
        _selectMediaBtn.setVisibility(View.VISIBLE);
    }

    private void enableSelectProfileMediaButton() {

        _defaultpic_enabled.setClickable(true);
        drawSelectProfileMediaButton(true);
    }

    private void disableSelectContactButton() {

        _selectContactBtn.setEnabled(false);
        drawSelectContactButton(false);

    }

    private void enableSelectContactButton() {

        _selectContactBtn.setEnabled(true);
        drawSelectContactButton(true);

    }

    private void enableMediaStatusArrived() {

        _mediaStatus.setVisibility(View.VISIBLE);
        _mediaStatus.bringToFront();
    }

    private void disableMediaStatusArrived() {

        _mediaStatus.setVisibility(View.INVISIBLE);
    }

    private void disableProgressBar() {

        _pBar.setVisibility(ProgressBar.GONE);
    }

    private void enableProgressBar() {

        if (_pBar != null)
            _pBar.setVisibility(ProgressBar.VISIBLE);
    }

    private void disableContactEditText() {

        _destinationEditText.setEnabled(false);
    }

    private void enableContactEditText() {

        _destinationEditText.setEnabled(true);
    }

    private void disableUserFetchProgressBar() {

        _fetchUserPbar.setVisibility(ProgressBar.GONE);
    }

    private void enableUserFetchProgressBar() {

        _fetchUserPbar.setVisibility(ProgressBar.VISIBLE);

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

        _inviteBtn.setVisibility(View.INVISIBLE);
        _inviteBtn.setClickable(false);
    }

    private void disableInviteButton() {

        _inviteBtn.setEnabled(false);
    }

    private void enableInviteButton() {

        _inviteBtn.setVisibility(View.VISIBLE);
        _inviteBtn.setClickable(true);
        _inviteBtn.bringToFront();
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

        _userStatusPositive.setVisibility(View.INVISIBLE);
    }

    private void disableUserStatusNegativeIcon() {

        _userStatusNegative.setVisibility(View.INVISIBLE);
    }

    private void enableUserStatusPositiveIcon() {

        _userStatusPositive.setVisibility(View.VISIBLE);
        _userStatusPositive.bringToFront();
    }

    private void enableUserStatusNegativeIcon() {

        _userStatusNegative.setVisibility(View.VISIBLE);
        _userStatusNegative.bringToFront();
    }

    private void disableRingToneStatusArrived() {

        _ringtoneStatus.setVisibility(View.INVISIBLE);
    }

    private void enableRingToneStatusArrived() {

        _ringtoneStatus.setVisibility(View.VISIBLE);
        _ringtoneStatus.bringToFront();
    }

    private void drawSelectMediaButton(boolean enabled) {

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.CALLER_MEDIA);
        try {
            FileManager.FileType fType;

            if (!enabled)
                _selectMediaBtn.setImageResource(R.drawable.select_profile_media_disabled);
            else {

                String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(getApplicationContext(), _destPhoneNumber);
                if (!lastUploadedMediaPath.equals("")) {
                    fType = FileManager.getFileType(lastUploadedMediaPath);

                    BitmapUtils.execBitMapWorkerTask(_selectMediaBtn, fType, lastUploadedMediaPath, false);

                    enableMediaStatusArrived();
                    // stretch the uploaded image as it won't stretch because we use a drawable instead that we don't want to stretch
                    _selectMediaBtn.setPadding(0, 0, 0, 0);
                    _selectMediaBtn.setScaleType(ImageView.ScaleType.FIT_XY);

                        showCaseViewAfterUploadAndCall();

                } else {// enabled but no uploaded media
                    String ringToneFilePath = lut_utils.getUploadedTonePerNumber(getApplicationContext(), _destPhoneNumber);
                    if (ringToneFilePath.isEmpty())
                     showCaseViewSelectMedia();
                    _selectMediaBtn.setImageResource(R.drawable.select_caller_media);
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

            if (!enabled) {
                BitmapUtils.execBitmapWorkerTask(_defaultpic_enabled, getApplicationContext(), getResources(), R.drawable.select_profile_media_disabled, true);
            } else {

                String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(getApplicationContext(), _destPhoneNumber);
                if (!lastUploadedMediaPath.equals("")) {
                    FileManager.FileType fType = FileManager.getFileType(lastUploadedMediaPath);

                    BitmapUtils.execBitMapWorkerTask(_defaultpic_enabled, fType, lastUploadedMediaPath, true);
                } else // enabled but no uploaded media

                    BitmapUtils.execBitmapWorkerTask(_defaultpic_enabled, getApplicationContext(), getResources(), R.drawable.select_profile_media_enabled, true);
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

        try {

            if (!ringToneFilePath.isEmpty()) {
                _ringToneNameTextView.setText(FileManager.getFileNameWithExtension(ringToneFilePath));
                _ringToneNameTextView.setVisibility(View.VISIBLE);

                enableRingToneStatusArrived();
                showCaseViewAfterUploadAndCall();
            } else {
                _ringToneNameTextView.setVisibility(View.INVISIBLE);

                disableRingToneStatusArrived();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to draw drawRingToneName:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

    private void disableRingToneName() {

        _ringToneNameTextView.setVisibility(View.INVISIBLE);
        disableRingToneStatusArrived();
    }

    private void drawSelectContactButton(boolean enabled) {

        if (enabled)
            _selectContactBtn.setImageResource(R.drawable.select_contact_enabled);
        else
            _selectContactBtn.setImageResource(R.drawable.select_contact_disabled);
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
                if (snackbarData.getText() != null && !snackbarData.getText().equals(""))
                    writeInfoSnackBar(snackbarData.getText(), snackbarData.getColor(), snackbarData.getmDuration());
                break;
        }
    }

    private void showMandatoryUpdateDialog(double appVersion) {

        MandatoryUpdateDialog mandatoryUpdateDialog = new MandatoryUpdateDialog(appVersion);
        mandatoryUpdateDialog.show(getSupportFragmentManager(), TAG);
    }

    //TODO change this to campaign API push for all users in case of last supported version change
    private void getAppRecord() {

        Intent i = new Intent(this, LogicServerProxyService.class);
        i.setAction(LogicServerProxyService.ACTION_GET_APP_RECORD);
        startService(i);
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

    //region Private classes
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