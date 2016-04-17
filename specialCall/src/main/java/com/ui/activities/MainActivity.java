package com.ui.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
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
import com.interfaces.ICallbackListener;
import com.mediacallz.app.R;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.ffmpeg4android.ProgressCalculator;
import com.netcompss.loader.LoadJNI;
import com.services.AbstractStandOutService;
import com.services.IncomingService;
import com.services.LogicServerProxyService;
import com.services.OutgoingService;
import com.services.PreviewService;
import com.services.StorageServerProxyService;
import com.ui.dialogs.MandatoryUpdateDialog;
import com.utils.BitmapUtils;
import com.utils.BroadcastUtils;
import com.utils.ContactsUtils;
import com.utils.FileCompressorUtil;
import com.utils.LUT_Utils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ClientObjects.ConnectionToServer;
import DataObjects.AppMetaRecord;
import DataObjects.SharedConstants;
import DataObjects.SpecialMediaType;
import DataObjects.TransferDetails;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;
import MessagesToServer.MessageUploadFile;
import utils.PhoneNumberUtils;

public class MainActivity extends AppCompatActivity implements OnClickListener, ICallbackListener {

    private final String TAG = MainActivity.class.getSimpleName();

    private String _destPhoneNumber = "";
    private String _destName = "";
    private LoadJNI _vk;
    private FileManager _fileForUpload;
    private SpecialMediaType _specialMediaType;

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
    private TextView _ringToneNameForProfileTextView;
    private ImageButton _inviteBtn;
    private RelativeLayout _mainActivityLayout;
    private ImageView _ringtoneStatus;
    private AutoCompleteTextView _destinationEditText;
    private TextView _destTextView;
    private TextView _mediaCallingTextView;
    private ProgressDialog _progDialog;
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

        //Copying FFMPEG license if necessary
        GeneralUtils.copyLicenseFromAssetsToSDIfNeeded(this, FileCompressorUtil.workFolder);

        startLoginActivityIfLoggedOut();

        prepareEventReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");

        String appState = getState();
        Log.i(TAG, "App State:" + appState);

        AppStateManager.setAppInForeground(getApplicationContext(), true);

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

            UI_Utils.showCaseViewCallNumber(getApplicationContext(), MainActivity.this);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");

        AppStateManager.setAppInForeground(getApplicationContext(), false);

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
                if (data != null) {
                    String msg = data.getStringExtra(SelectMediaActivity.RESULT_ERR_MSG);
                    if (msg != null) {
                        SnackbarData snackbarData = new SnackbarData(SnackbarData.SnackbarStatus.SHOW,
                                Color.RED,
                                Snackbar.LENGTH_INDEFINITE,
                                data.getStringExtra(SelectMediaActivity.RESULT_ERR_MSG));
                        writeInfoSnackBar(snackbarData);
                    }
                    else
                    {
                        _specialMediaType = (SpecialMediaType) data.getSerializableExtra(SelectMediaActivity.RESULT_SPECIAL_MEDIA_TYPE);
                        FileManager fm = (FileManager) data.getSerializableExtra(SelectMediaActivity.RESULT_FILE);

                        if(FileCompressorUtil.isCompressionNeeded(fm)) {
                            TrimTask trimTask = new TrimTask(fm);
                            trimTask.execute();
                        }
                        else
                        {
                            _fileForUpload = fm;
                            Intent i = new Intent(getApplicationContext(), StorageServerProxyService.class);
                            i.setAction(StorageServerProxyService.ACTION_UPLOAD);
                            i.putExtra(StorageServerProxyService.DESTINATION_ID, _destPhoneNumber);
                            i.putExtra(StorageServerProxyService.SPECIAL_MEDIA_TYPE, _specialMediaType);
                            i.putExtra(StorageServerProxyService.FILE_TO_UPLOAD, _fileForUpload);
                            startService(i);
                        }
                    }
                }

            }
            else if (requestCode == ActivityRequestCodes.SELECT_CONTACT) {
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


        AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME, am.getStreamVolume(AudioManager.STREAM_MUSIC));
        Log.i(TAG, "PreviewStart MUSIC_VOLUME Original" + String.valueOf(am.getStreamVolume(AudioManager.STREAM_MUSIC)));


        // close previous
        Intent closePrevious = new Intent(getApplicationContext(), PreviewService.class);
        closePrevious.setAction(AbstractStandOutService.ACTION_CLOSE_ALL);
        startService(closePrevious);

        LUT_Utils lut_utils = new LUT_Utils(specialMediaType);
        Intent showPreview = new Intent(getApplicationContext(), PreviewService.class);
        showPreview.setAction(AbstractStandOutService.ACTION_PREVIEW);

        showPreview.putExtra(AbstractStandOutService.PREVIEW_AUDIO, lut_utils.getUploadedTonePerNumber(getApplicationContext(), _destPhoneNumber));
        showPreview.putExtra(AbstractStandOutService.PREVIEW_VISUAL_MEDIA, lut_utils.getUploadedMediaPerNumber(getApplicationContext(), _destPhoneNumber));


        startService(showPreview);


    }

    private void selectMedia(int specialMediaType) {

        /* Create an intent that will start the main activity. */
        Intent mainIntent = new Intent(this, SelectMediaActivity.class);
        mainIntent.putExtra(SelectMediaActivity.SPECIAL_MEDIA_TYPE, specialMediaType);
        mainIntent.putExtra(SelectMediaActivity.DESTINATION_NUMBER, _destPhoneNumber);
        mainIntent.putExtra(SelectMediaActivity.DESTINATION_NAME, _destName);
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

                Intent intent = new Intent(Intent.ACTION_SENDTO,
                        Uri.fromParts("sms", callNumber.getText().toString(), null));
                intent.putExtra("sms_body", getResources().getString(R.string.invite));
                startActivity(intent);


            } catch (Exception ex) {
                Log.e(TAG, "Failed to open send SMS activity. [Exception]:" + (ex.getMessage() != null ? ex.getMessage() : ex));
            }
        }
    }

    public void eventReceived(Event event) {

        final EventReport report = event.report();

        switch (report.status()) {

            case TRIMMING_COMPLETE:
                CompressTask compressTask = new CompressTask(_fileForUpload);
                compressTask.execute();
                break;

            case COMPRESSION_COMPLETE:
                Intent i = new Intent(getApplicationContext(), StorageServerProxyService.class);
                i.setAction(StorageServerProxyService.ACTION_UPLOAD);
                i.putExtra(StorageServerProxyService.DESTINATION_ID, _destPhoneNumber);
                i.putExtra(StorageServerProxyService.SPECIAL_MEDIA_TYPE, _specialMediaType);
                i.putExtra(StorageServerProxyService.FILE_TO_UPLOAD, _fileForUpload);
                startService(i);
                break;

            case UPLOADING:
                TransferDetails td = (TransferDetails) report.data();
                ConnectionToServer conn  = StorageServerProxyService.getConn();
                UploadTask uploadTask = new UploadTask(conn, td);
                uploadTask.execute();
                break;

            case APP_RECORD_RECEIVED:
                AppMetaRecord appMetaRecord = (AppMetaRecord) report.data();

                if (SharedConstants.APP_VERSION < appMetaRecord.get_lastSupportedVersion())
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
                        Snackbar.LENGTH_LONG,
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
        if (_autoCompleteTextViewDestPhone != null) {
            _destPhoneNumber = _autoCompleteTextViewDestPhone.getText().toString();
            SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER, _destPhoneNumber);
        }

        // Saving destination name
        if (_destTextView != null && (!_destTextView.getText().toString().isEmpty())) {
            _destName = _destTextView.getText().toString();
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
        String destNumber = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER);
        if (_autoCompleteTextViewDestPhone != null && destNumber != null)
            _autoCompleteTextViewDestPhone.setText(destNumber);

        // Restoring destination name
        _destName = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME);
        setDestNameTextView();
    }

    private void setDestNameTextView() {

        if (_destTextView != null) {

            if (_destName != null && !_destName.equals(""))
                _destTextView.setText(_destName);
            else if (_destPhoneNumber != null && !_destPhoneNumber.equals(""))
                _destTextView.setText(_destPhoneNumber);
            else {
                disableDestinationTextView();
                return;
            }

            enableDestinationTextView();
        }
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

                            SnackbarData snackbarData = new SnackbarData(
                                    SnackbarData.SnackbarStatus.SHOW,
                                    Color.YELLOW,
                                    Snackbar.LENGTH_LONG,
                                    getResources().getString(R.string.cant_send_to_self));

                            writeInfoSnackBar(snackbarData);
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

        prepareDestNameTextView();
        prepareMediaCallingTextView();
        prepareUserStatusNegative();
        prepareUserStatusPositive();
        prepareDestinationEditText();
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
        enableDestinationEditText();
        disableUserFetchProgressBar();
        disableSelectProfileMediaButton();
        disableSelectCallerMediaButton();
        disableRingToneName();
        disableRingToneNameForProfile();
        disableCallButton();
        disableMediaCallingTextView();
        disableDestinationTextView();

    }

    public void stateReady() {

        disableProgressBar();
        enableSelectMediaButton();
        drawRingToneName();
        drawRingToneNameForProfile();
        disableUserFetchProgressBar();
        enableSelectProfileMediaButton();
        enableDestinationEditText();
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
        disableDestinationEditText();
        disableCallButton();
        disableInviteButton();

    }

    public void stateLoading() {

        if (AppStateManager.isLoadingStateActive()) {

            disableSelectCallerMediaButton();
            disableSelectProfileMediaButton();
            disableSelectContactButton();
            disableDestinationEditText();
            disableCallButton();
        } else {
            AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.getAppPrevState(getApplicationContext()));
            syncUIwithAppState();
        }

    }

    private String getState() {

        return AppStateManager.getAppState(getApplicationContext());
    }

    private void syncUIwithAppState() {

        String appState = getState();

        Log.i(TAG, "Syncing UI with appState:" + appState);

        switch (appState) {

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
    private void prepareMediaCallingTextView() {

        _mediaCallingTextView = (TextView) findViewById(R.id.media_calling);
    }

    private void prepareUserStatusPositive() {

        _userStatusPositive = (ImageView) findViewById(R.id.userStatusPositive);
    }

    private void prepareUserStatusNegative() {

        _userStatusNegative = (ImageView) findViewById(R.id.userStatusNegative);
    }

    private void prepareDestinationEditText() {

        _destinationEditText = (AutoCompleteTextView) findViewById(R.id.CallNumber);
    }

    private void prepareDestNameTextView() {

        _destTextView = (TextView) findViewById(R.id.destName);
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
        _ringToneNameForProfileTextView = (TextView) findViewById(R.id.ringtoneNameForProfile);
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

        dataList.add(new DrawerItem(getResources().getString(R.string.media_management), R.drawable.mediaicon));
        dataList.add(new DrawerItem(getResources().getString(R.string.who_can_mc_me), R.drawable.blackwhitelist));
//        dataList.add(new DrawerItem("How To ?", R.drawable.questionmark));
//        dataList.add(new DrawerItem("Share Us", R.drawable.shareus));
//        dataList.add(new DrawerItem("Rate Us", R.drawable.rateus2));
        dataList.add(new DrawerItem(getResources().getString(R.string.app_settings), R.drawable.settingsicon));
        dataList.add(new DrawerItem(getResources().getString(R.string.about_FAQ), R.drawable.color_mc));

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
            case 2: // App Settings
                appSettings();
                break;
            case 3: // About & Help
                appAboutAndHelp();

                break;

            default:
                break;
        }

        _mDrawerLayout.closeDrawer(_DrawerList);

    }

    private void appSettings() {

        saveInstanceState();
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, SetSettingsActivity.class);
        startActivity(intent);
    }

    private void appAboutAndHelp() {

        saveInstanceState();
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, SetAboutHelpActivity.class);
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
        _ringToneNameForProfileTextView.setVisibility(View.INVISIBLE);
    }

    private void enableSelectMediaButton() {

        _selectMediaBtn.setClickable(true);

        // make the drawable fit center and with padding, so it won't stretch
        int sizeInDp = 30;
        float scale = getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (sizeInDp * scale + 0.5f);
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

    private void disableDestinationTextView() {

        _destTextView.setText("");
        _destTextView.setVisibility(TextView.INVISIBLE);
        disableMediaCallingTextView();
    }

    private void enableDestinationTextView() {

        _destTextView.setVisibility(TextView.VISIBLE);
        enableMediaCallingTextView();
    }

    private void disableMediaCallingTextView() {

        _mediaCallingTextView.setVisibility(TextView.INVISIBLE);
    }

    private void enableMediaCallingTextView() {

        _mediaCallingTextView.setVisibility(TextView.VISIBLE);
    }

    private void disableDestinationEditText() {

        _destinationEditText.setEnabled(false);
    }

    private void enableDestinationEditText() {

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

                    UI_Utils.showCaseViewAfterUploadAndCall(getApplicationContext(), MainActivity.this);

                } else {// enabled but no uploaded media
                    String ringToneFilePath = lut_utils.getUploadedTonePerNumber(getApplicationContext(), _destPhoneNumber);
                    if (ringToneFilePath.isEmpty())
                        UI_Utils.showCaseViewSelectMedia(getApplicationContext(), MainActivity.this);
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
                UI_Utils.showCaseViewAfterUploadAndCall(getApplicationContext(), MainActivity.this);
            } else {
                _ringToneNameTextView.setVisibility(View.INVISIBLE);

                disableRingToneStatusArrived();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to draw drawRingToneName:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

    private void drawRingToneNameForProfile() {

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.PROFILE_MEDIA);
        String ringToneFilePath = lut_utils.getUploadedTonePerNumber(getApplicationContext(), _destPhoneNumber);

        try {

            if (!ringToneFilePath.isEmpty()) {
                _ringToneNameForProfileTextView.setText(FileManager.getFileNameWithExtension(ringToneFilePath));
                _ringToneNameForProfileTextView.setVisibility(View.VISIBLE);

                UI_Utils.showCaseViewAfterUploadAndCall(getApplicationContext(), MainActivity.this);
            } else {
                _ringToneNameForProfileTextView.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to draw drawRingToneNameForProfile:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

    private void disableRingToneName() {

        _ringToneNameTextView.setVisibility(View.INVISIBLE);
        disableRingToneStatusArrived();
    }

    private void disableRingToneNameForProfile() {

        _ringToneNameForProfileTextView.setVisibility(View.INVISIBLE);

    }

    private void drawSelectContactButton(boolean enabled) {

        if (enabled)
            _selectContactBtn.setImageResource(R.drawable.select_contact_enabled);
        else
            _selectContactBtn.setImageResource(R.drawable.select_contact_disabled);
    }

    private void writeInfoSnackBar(final SnackbarData snackBarData) {

        Log.i(TAG, "Snackbar showing:" + snackBarData.getText());

        int duration = snackBarData.getDuration();
        if (duration == Snackbar.LENGTH_LONG)
            duration = 3500;

        View mainActivity = findViewById(R.id.mainActivity);

        if (mainActivity != null && snackBarData.getText() != null) {
            final Snackbar snackbar = Snackbar
                    .make(mainActivity, Html.fromHtml(snackBarData.getText()), duration)
                    .setActionTextColor(snackBarData.getColor());
            snackbar.setAction(R.string.close, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackbar.dismiss();
                }
            });

            if (snackBarData.isLoading()) {
                Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
                snackbarLayout.addView(new ProgressBar(getApplicationContext()));
            }
            snackbar.show();
        }
    }

    private void handleSnackBar(SnackbarData snackbarData) {

        switch (snackbarData.getStatus()) {
            case CLOSE:
                break;

            case SHOW:
                if (snackbarData.getText() != null && !snackbarData.getText().equals(""))
                    writeInfoSnackBar(snackbarData);
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

    //region Handlers
    private Handler _compressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "Handler got message:" + msg.what);
            if (_progDialog != null) {
                _progDialog.dismiss();

                // Stopping the transcoding native
                if (msg.what == FileCompressorUtil.STOP_TRANSCODING_MSG) {
                    Log.i(TAG, "Got cancel message, calling fexit");
                    _vk.fExit(getApplicationContext());


                }
            }
        }
    };
    //endregion

    //region Private classes and AsyncTasks
    private class DrawerItemClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            if (position != 0)
                selectNavigationItem(position);
        }
    }


    private class UploadTask extends AsyncTask<Void,Integer,Void> {
        private final String TAG = UploadTask.class.getSimpleName();
        private ConnectionToServer _connectionToServer;
        private TransferDetails _td;
        private ProgressDialog _progDialog;
        private UploadTask _taskInstance;
        private BufferedInputStream _bis;

        public UploadTask(ConnectionToServer connectionToServer, TransferDetails td) {

            _connectionToServer = connectionToServer;
            _td = td;
            _taskInstance = this;

        }

        @Override
        protected void onPreExecute() {

            String cancel = getResources().getString(R.string.cancel);

            _progDialog = new ProgressDialog(MainActivity.this);
            _progDialog.setIndeterminate(false);
            _progDialog.setCancelable(false);
            _progDialog.setTitle(getResources().getString(R.string.uploading));
            _progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            _progDialog.setProgress(0);
            _progDialog.setMax((int)_td.getFileSize());
            _progDialog.setButton(DialogInterface.BUTTON_NEGATIVE, cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    _taskInstance.cancel(true);
                }
            });

            _progDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            FileManager managedFile = _td.get_managedFile();
            MessageUploadFile msgUF = new MessageUploadFile(_td.getSourceId(),_td);

            DataOutputStream dos;
            try {
                _connectionToServer.sendToServer(msgUF);

                Log.i(TAG, "Initiating file data upload...");

                dos = new DataOutputStream(_connectionToServer.getClientSocket().getOutputStream());

                FileInputStream fis = new FileInputStream(managedFile.getFile());
                _bis = new BufferedInputStream(fis);

                byte[] buf = new byte[1024 * 8];
                long fileSize = managedFile.getFileSize();
                long bytesToRead = fileSize;
                int bytesRead;
                while (bytesToRead > 0 && (bytesRead = _bis.read(buf, 0, (int) Math.min(buf.length, bytesToRead))) != -1 && !isCancelled()) {
                    dos.write(buf, 0, bytesRead);
                    //float percent = (float) ((fileSize - bytesToRead) / fileSize) * 100;
                    publishProgress((int)bytesRead);
                    bytesToRead -= bytesRead;
                }

                if(_progDialog!=null && _progDialog.isShowing()) {
                    _progDialog.dismiss();
                }

            }
            catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed:" + e.getMessage());
                BroadcastUtils.sendEventReportBroadcast(MainActivity.this, TAG,
                        new EventReport(EventType.STORAGE_ACTION_FAILURE, "Upload to " + _td.getDestinationId() + " failed:" + e.getMessage(), null));
            } finally {

                if(_bis!=null) {
                    try {
                        _bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Log.i(TAG, "Deleting "+_td.getDestinationId()+"'s temp compressed folder after upload");
                File tempCompressedDir = new File(Constants.TEMP_COMPRESSED_FOLDER +_td.getDestinationId());

                String[] entries = tempCompressedDir.list();
                for (String s : entries) {
                    File currentFile = new File(tempCompressedDir.getPath(), s);
                    currentFile.delete();
                }
            }

            return null;
        }

        @Override
        protected void onCancelled() {

            Intent i = new Intent(MainActivity.this, StorageServerProxyService.class);
            i.setAction(StorageServerProxyService.ACTION_CANCEL);
            startService(i);

            if(_bis!=null) {
                try {
                    _bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.LOADING_CANCEL, null, null));
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {

            if(_progDialog!=null) {
                _progDialog.incrementProgressBy(progress[0]);
            }
        }

        @Override
        protected void onPostExecute(Void result)    {

            try {
                Thread.sleep(1000); // Sleeping so in fast uploads the dialog won't appear and disappear too fast (link a blink)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(_progDialog!=null && _progDialog.isShowing()) {
                _progDialog.dismiss();
            }

            String msg = MainActivity.this.getResources().getString(R.string.upload_success);
            // Setting state
            AppStateManager.setAppState(MainActivity.this, TAG, AppStateManager.STATE_READY);

            // Setting parameters for snackbar message
            int color = Color.GREEN;
            int sBarDuration = Snackbar.LENGTH_LONG;

            UI_Utils.showSnackBar(msg, color, sBarDuration, false, MainActivity.this);
        }

    }

    private class CompressTask extends AsyncTask<Void, Integer, Void> {

        private FileCompressorUtil _fileCompressor;
        private FileManager _baseFile;
        private final String TAG = CompressTask.class.getSimpleName();
        private CompressTask _instance = this;

        public CompressTask(FileManager baseFile) {

            _baseFile = baseFile;
        }

        @Override
        protected void onPreExecute() {

            String cancel = getResources().getString(R.string.cancel);

            _progDialog = new ProgressDialog(MainActivity.this);
            _progDialog.setIndeterminate(false);
            _progDialog.setCancelable(false);
            _progDialog.setTitle(getResources().getString(R.string.compressing_file));
            _progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            _progDialog.setProgress(0);
            _progDialog.setMax(100);
            _progDialog.setButton(DialogInterface.BUTTON_NEGATIVE, cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    _compressHandler.sendEmptyMessage(FileCompressorUtil.STOP_TRANSCODING_MSG);
                    _instance.cancel(true);
                }
            });

            _progDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            // Worker thread
            new Thread() {
                public void run() {
                    Log.d(TAG, "Worker started");

                    _vk = new LoadJNI();
                    PowerManager powerManager = (PowerManager)MainActivity.this.getSystemService(Activity.POWER_SERVICE);
                    PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VK_LOCK");

                    _fileCompressor = new FileCompressorUtil(_vk, wakeLock);
                    _fileForUpload = _fileCompressor.compressFileIfNecessary(_baseFile, _destPhoneNumber, getApplicationContext());
                    _compressHandler.sendEmptyMessage(FileCompressorUtil.FINISHED_TRANSCODING_MSG);
                    if(isCancelled()) {
                        BroadcastUtils.sendEventReportBroadcast(MainActivity.this, TAG, new EventReport(EventType.LOADING_CANCEL, null, null));
                        return;
                    }

                    BroadcastUtils.sendEventReportBroadcast(MainActivity.this, TAG, new EventReport(EventType.COMPRESSION_COMPLETE, null, null));
                }
            }.start();

            // Progress update thread
            new Thread() {
                ProgressCalculator pc = new ProgressCalculator(FileCompressorUtil.VK_LOG_PATH);
                public void run() {
                    Log.d(TAG,"Progress update started");
                    int progress = -1;
                    try {
                        while (true) {
                            sleep(300);
                            progress = pc.calcProgress();
                            if (progress != 0 && progress < 100) {
                                _progDialog.setProgress(progress);
                            }
                            else if (progress == 100) {
                                Log.i(TAG, "Progress is 100, exiting progress update thread");
                                pc.initCalcParamsForNextInter();
                                break;
                            }
                        }

                    } catch(Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            }.start();

            return null;
        }
    }

    private class TrimTask extends AsyncTask<Void, Integer, Void> {

        private FileCompressorUtil _fileCompressor;
        private FileManager _baseFile;
        private final String TAG = CompressTask.class.getSimpleName();
        TrimTask _instance = this;

        public TrimTask(FileManager baseFile) {

            _baseFile = baseFile;
        }

        @Override
        protected void onPreExecute() {

            String cancel = getResources().getString(R.string.cancel);

            _progDialog = new ProgressDialog(MainActivity.this);
            _progDialog.setIndeterminate(false);
            _progDialog.setCancelable(false);
            _progDialog.setTitle(getResources().getString(R.string.trimming_file));
            _progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            _progDialog.setProgress(0);
            _progDialog.setMax(100);
            _progDialog.setButton(DialogInterface.BUTTON_NEGATIVE, cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    _compressHandler.sendEmptyMessage(FileCompressorUtil.STOP_TRANSCODING_MSG);
                    _instance.cancel(true);
                }
            });

            _progDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            // Worker thread
            new Thread() {
                public void run() {
                    Log.d(TAG, "Worker started");

                    _vk = new LoadJNI();
                    PowerManager powerManager = (PowerManager)MainActivity.this.getSystemService(Activity.POWER_SERVICE);
                    PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VK_LOCK");

                    _fileCompressor = new FileCompressorUtil(_vk, wakeLock);
                    _fileForUpload = _fileCompressor.trimFileIfNecessary(_baseFile, _destPhoneNumber, getApplicationContext());
                    _compressHandler.sendEmptyMessage(FileCompressorUtil.FINISHED_TRANSCODING_MSG);
                    if(isCancelled()) {
                        BroadcastUtils.sendEventReportBroadcast(MainActivity.this, TAG, new EventReport(EventType.LOADING_CANCEL, null, null));
                        return;
                    }

                    BroadcastUtils.sendEventReportBroadcast(MainActivity.this, TAG, new EventReport(EventType.TRIMMING_COMPLETE, null, null));
                }
            }.start();

            // Progress update thread
            new Thread() {
                ProgressCalculator pc = new ProgressCalculator(FileCompressorUtil.VK_LOG_PATH);
                public void run() {
                    Log.d(TAG,"Progress update started");
                    int progress = -1;
                    try {
                        while (true) {
                            sleep(300);
                            progress = pc.calcProgress();
                            if (progress != 0 && progress < 100) {
                                _progDialog.setProgress(progress);
                            }
                            else if (progress == 100) {
                                Log.i(TAG, "Progress is 100, exiting progress update thread");
                                pc.initCalcParamsForNextInter();
                                break;
                            }
                        }

                    } catch(Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            }.start();

            return null;
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