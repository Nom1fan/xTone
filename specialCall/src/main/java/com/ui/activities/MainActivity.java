package com.ui.activities;

import android.app.Activity;
import android.app.Dialog;
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
import android.os.Build;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actions.ActionFactory;
import com.actions.ClientAction;
import com.app.AppStateManager;
import com.async_tasks.AutoCompletePopulateListAsyncTask;
import com.async_tasks.IsRegisteredTask;
import com.batch.android.Batch;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
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
import com.ui.dialogs.ClearMediaDialog;
import com.ui.dialogs.InviteDialog;
import com.ui.dialogs.MandatoryUpdateDialog;
import com.utils.BitmapUtils;
import com.utils.BroadcastUtils;
import com.utils.ContactsUtils;
import com.utils.FFMPEG_Utils;
import com.utils.FileCompressorUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import ClientObjects.ConnectionToServer;
import ClientObjects.IServerProxy;
import DataObjects.DataKeys;
import DataObjects.SharedConstants;
import DataObjects.SpecialMediaType;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;
import MessagesToClient.MessageToClient;
import MessagesToServer.MessageToServer;
import MessagesToServer.ServerActionType;
import utils.PhoneNumberUtils;

import static com.crashlytics.android.Crashlytics.log;
import static com.crashlytics.android.Crashlytics.setUserIdentifier;

public class MainActivity extends AppCompatActivity implements OnClickListener, ICallbackListener {

    private final String TAG = MainActivity.class.getSimpleName();

    private String _destPhoneNumber = "";
    private String _destName = "";
    private LoadJNI _vk;
    private final Object _lock = new Object();

    //region Keys for bundle
    private static final String DEST_ID             =   "DEST_ID";
    private static final String DEST_NAME           =   "DEST_NAME";
    private static final String FILE_FOR_UPLOAD     =   "FILE_FOR_UPLOAD";
    private static final String SPEC_MEDIA_TYPE     =   "SPEC_MEDIA_TYPE";
    //endregion

    //region UI elements
    private ImageButton _selectContactBtn;
    private ImageButton _selectMediaBtn;
    private ImageButton _selectMediaBtn_small;
    private TextView _selectMediaBtn_textview;
    private TextView _selectMediaBtn_textview2;
  //  private TextView _callBtn_textview;
    private ImageButton _callBtn;
    private ImageButton _clearText;
    private ProgressBar _fetchUserPbar;
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
    private TextView _profile_textview;
    private TextView _profile_textview2;
    private RelativeLayout _mainActivityLayout;
    private ImageView _ringtoneStatus;
    private AutoCompleteTextView _destinationEditText;
    private TextView _destTextView;
    private boolean _profileHasMedia = false;
    private boolean _callerHasMedia = false;
    private boolean _profileHasRingtone = false;
    private boolean _callerHasRingtone = false;
    private volatile boolean _contCalcProgress = false;
    private volatile boolean _updateThreadNextIterStarted = false;
    private ProgressDialog _progDialog;
    private Snackbar _snackBar;
    private Dialog _tipDialog = null;
    private String[] _tipsCircularArray;
    private int _tipsNum;
    //endregion

    //region Handlers
    private Handler _compressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            log(Log.INFO,TAG, "Handler got message:" + msg.what);

            // Stopping the transcoding native
            if (msg.what == FileCompressorUtils.STOP_TRANSCODING_MSG) {
                log(Log.INFO,TAG, "Got cancel message, calling fexit");
                if (_progDialog != null)
                    _progDialog.dismiss();

                _vk.fExit(getApplicationContext());

                wakeUpdateThreadToFinish();
            }
            else if (msg.what == FileCompressorUtils.FINISHED_TRANSCODING_MSG) {

                wakeUpdateThreadToFinish();

                if(_progDialog != null)
                    _progDialog.dismiss();
            }
            else if(msg.what == FileCompressorUtils.COMPRESSION_PHASE_2) {

                log(Log.INFO,TAG, "Got compression phase 2 message");

                if(_progDialog!=null) {

                    String str = getResources().getString(R.string.compressing_file2);
                    _progDialog.setProgress(0);
                    _progDialog.setTitle(str);
                }

                wakeUpdateThreadToContinue();
            }
        }

        private void wakeUpdateThreadToFinish() {

            _contCalcProgress = false;
            _updateThreadNextIterStarted = true;
            synchronized (_lock) {
                _lock.notify();
            }
        }

        private void wakeUpdateThreadToContinue() {

            _updateThreadNextIterStarted = true;
            synchronized (_lock) {
                _lock.notify();
            }
        }
    };
    //endregion

    //region Activity methods (onCreate(), onPause(), ...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        startLoginActivityIfLoggedOut();

        initializeUI();

        if (SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.SELECT_MEDIA_VIEW) && SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW))
            startingTipDialog();
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
        log(Log.INFO,TAG, "onStart()");

        Batch.onStart(this);

        //Copying FFMPEG license if necessary
        GeneralUtils.copyLicenseFromAssetsToSDIfNeeded(this, FileCompressorUtils.workFolder);

        startLoginActivityIfLoggedOut();

        prepareEventReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        log(Log.INFO,TAG, "onResume()");
        setUserIdentifier(SharedPrefUtils.getString(getApplicationContext(),SharedPrefUtils.GENERAL,SharedPrefUtils.LOGIN_NUMBER));

        String appState = getState();
        log(Log.INFO,TAG, "App State:" + appState);

        AppStateManager.setAppInForeground(getApplicationContext(), true);

        if (AppStateManager.isLoggedIn(this)) {

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

            // Taking Focus from AutoCompleteTextView in the end, so it won't pop up :) Also added focus capabilities to the activity_main.xml
            _mainActivityLayout.requestFocus();

            prepareEventReceiver();

            // ASYNC TASK To Populate all contacts , it can take long time and it delays the UI
            new AutoCompletePopulateListAsyncTask(_autoCompleteTextViewDestPhone).execute(getApplicationContext());

            if (!appState.equals(AppStateManager.STATE_LOADING) && !appState.equals(AppStateManager.STATE_DISABLED))
                handleSnackBar(new SnackbarData(SnackbarData.SnackbarStatus.CLOSE, 0 ,0 ,null));

            restoreInstanceState();

            getAppRecord();

            syncAndroidVersionWithServer();

            UI_Utils.showCaseViewCallNumber(this, MainActivity.this);



        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        log(Log.INFO,TAG, "onPause()");

        SharedPrefUtils.setBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION, false);
        AppStateManager.setAppInForeground(this, false);

        if (_eventReceiver != null) {
            try {
                unregisterReceiver(_eventReceiver);
            } catch (Exception ex) {
                log(Log.ERROR,TAG, ex.getMessage());
            }
        }
        saveInstanceState();

//        UI_Utils.unbindDrawables(findViewById(R.id.mainActivity));
//        System.gc();
    }

    @Override
    protected void onStop() {
        log(Log.INFO,TAG, "onStop()");
        Batch.onStop(this);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        log(Log.INFO,TAG, "onDestroy()");

      if (AppStateManager.getAppState(this).equals(AppStateManager.STATE_LOADING))
          AppStateManager.setAppState(this, TAG, AppStateManager.getAppPrevState(this));

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
                    } else {
                        SpecialMediaType _specialMediaType = (SpecialMediaType) data.getSerializableExtra(SelectMediaActivity.RESULT_SPECIAL_MEDIA_TYPE);
                        FileManager fm = (FileManager) data.getSerializableExtra(SelectMediaActivity.RESULT_FILE);

                        Bundle bundle = new Bundle();
                        bundle.putSerializable(FILE_FOR_UPLOAD, fm);
                        bundle.putString(DEST_ID, _destPhoneNumber);
                        bundle.putString(DEST_NAME, _destName);
                        bundle.putSerializable(SPEC_MEDIA_TYPE, _specialMediaType);

                        if (FileCompressorUtils.isCompressionNeeded(fm)) {
                            File tempCompressedDir = new File(Constants.TEMP_COMPRESSED_FOLDER + _destPhoneNumber);
                            tempCompressedDir.mkdir();
                            TrimTask trimTask = new TrimTask(bundle);
                            trimTask.execute();
                        } else {

                            executeUploadTask(bundle);
                        }
                    }
                }

            } else if (requestCode == ActivityRequestCodes.SELECT_CONTACT) {
                try {
                    if (data != null) {
                        Uri uri = data.getData();
                        Contact contact = ContactsUtils.getContact(uri, this);
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
    //endregion (on

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (_mDrawerToggle != null)
            if (_mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }

        return true;
    }

    //region Assisting methods (onClick(), eventReceived(), ...)
    private void startingTipDialog(){

        if (!SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_TIP)) {


            _tipsCircularArray = new String[] {
                    getApplicationContext().getResources().getString(R.string.tip1_windowresize),
                    getApplicationContext().getResources().getString(R.string.tip2_profile),
                    getApplicationContext().getResources().getString(R.string.tip3_block),
                    getApplicationContext().getResources().getString(R.string.tip4_image_audio_gif),
                    getApplicationContext().getResources().getString(R.string.tip5_you_can_preview)
            };

            Random r = new Random();
            _tipsNum = r.nextInt(5);

            final int arrayLength = _tipsCircularArray.length;

         if (_tipDialog==null)
             {  _tipDialog = new Dialog(MainActivity.this);

                    // custom dialog

                    _tipDialog.setContentView(R.layout.tip_dialog);

                    // set the custom dialog components - text, image and button
                    final TextView text = (TextView) _tipDialog.findViewById(R.id.tip_msg);
                    text.setText(_tipsCircularArray[_tipsNum % arrayLength]);


                    Button nextTipBtn = (Button) _tipDialog.findViewById(R.id.next_tip);
                    // if button is clicked, close the custom dialog
                    nextTipBtn.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            text.setText(_tipsCircularArray[_tipsNum++ % arrayLength]);
                        }
                    });

                     _tipDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                         @Override
                         public void onDismiss(final DialogInterface arg0) {
                             _tipDialog = null;
                         }
                     });

                    Button skipBtn = (Button) _tipDialog.findViewById(R.id.skip);
                    // if button is clicked, close the custom dialog
                    skipBtn.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            _tipDialog.dismiss();
                        }
                    });

                    CheckBox checkBox = (CheckBox) _tipDialog.findViewById(R.id.dont_show_tips);
                    checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                            SharedPrefUtils.setBoolean(MainActivity.this, SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_TIP, isChecked);

                        }
                    });
                    checkBox.setText(MainActivity.this.getResources().getString(R.string.dont_show_again));


                    _tipDialog.show();


                }


        }
    }

    private void startLoginActivityIfLoggedOut() {

        if (!AppStateManager.isLoggedIn(this)) {

            stateLoggedOut();
        }
    }

    private void startPreviewStandoutWindow(SpecialMediaType specialMediaType) {

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        SharedPrefUtils.setInt(this, SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME, am.getStreamVolume(AudioManager.STREAM_MUSIC));
        log(Log.INFO,TAG, "PreviewStart MUSIC_VOLUME Original" + String.valueOf(am.getStreamVolume(AudioManager.STREAM_MUSIC)));

        // Close previous
        Intent closePrevious = new Intent(this, PreviewService.class);
        closePrevious.setAction(AbstractStandOutService.ACTION_CLOSE_ALL);
        startService(closePrevious);

        LUT_Utils lut_utils = new LUT_Utils(specialMediaType);
        Intent showPreview = new Intent(this, PreviewService.class);
        showPreview.setAction(AbstractStandOutService.ACTION_PREVIEW);
        showPreview.putExtra(AbstractStandOutService.PREVIEW_AUDIO, lut_utils.getUploadedTonePerNumber(this, _destPhoneNumber));
        showPreview.putExtra(AbstractStandOutService.PREVIEW_VISUAL_MEDIA, lut_utils.getUploadedMediaPerNumber(this, _destPhoneNumber));

        startService(showPreview);
    }

    private void executeUploadTask(Bundle bundle) {

        UploadTask uploadTask = new UploadTask(bundle);
        uploadTask.execute();
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
            if (_callerHasMedia || _callerHasRingtone)
                openCallerMediaMenu();
            else
                selectMedia(ActivityRequestCodes.SELECT_CALLER_MEDIA);

        }  else if (id == R.id.selectmedia_btn_small) {
            if (_callerHasMedia || _callerHasRingtone)
                openCallerMediaMenu();
            else
                selectMedia(ActivityRequestCodes.SELECT_CALLER_MEDIA);

        }else if (id == R.id.selectProfileMediaBtn) {

            if (_profileHasMedia || _profileHasRingtone)
                openProfileMediaMenu();
            else
                selectMedia(ActivityRequestCodes.SELECT_PROFILE_MEDIA);

        } else if (id == R.id.selectContactBtn) {

            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            startActivityForResult(intent, ActivityRequestCodes.SELECT_CONTACT);

        } else if (id == R.id.clear) {

            SharedPrefUtils.setBoolean(this,SharedPrefUtils.GENERAL,SharedPrefUtils.DISABLE_UI_ELEMENTS_ANIMATION, true);
            AutoCompleteTextView textViewToClear = (AutoCompleteTextView) findViewById(R.id.CallNumber);
            textViewToClear.setText("");

        }
    }

    public void eventReceived(Event event) {

        final EventReport report = event.report();

        switch (report.status()) {

            case APP_RECORD_RECEIVED: {
                HashMap<DataKeys, Object> data = (HashMap) report.data();

                if (Constants.APP_VERSION(this) < (double) data.get(DataKeys.MIN_SUPPORTED_VERSION))
                   showMandatoryUpdateDialog();
            }
                break;

            case USER_REGISTERED_FALSE:
                InviteDialog inviteDialog = new InviteDialog();
                inviteDialog.show(getFragmentManager(), TAG);
                break;

            case CLEAR_SENT:
                if(!SharedPrefUtils.getBoolean(MainActivity.this ,SharedPrefUtils.GENERAL,SharedPrefUtils.DONT_SHOW_AGAIN_CLEAR_DIALOG)) {
                    UI_Utils.showWaitingForTranferSuccussDialog(MainActivity.this ,"ClearMediaDialog",getResources().getString(R.string.sending_clear_contact)
                            ,getResources().getString(R.string.waiting_for_clear_transfer_sucess_dialog_msg));
                }
                break;

            case REFRESH_UI:
                SnackbarData data = (SnackbarData) report.data();
                syncUIwithAppState();

                if (data != null)
                    handleSnackBar(data);
                break;

            default: // Event not meant for MainActivity receiver
        }
    }

    //TODO change this to campaign API push for all users in case of last supported version change
    private void getAppRecord() {

        Intent i = new Intent(this, LogicServerProxyService.class);
        i.setAction(LogicServerProxyService.ACTION_GET_APP_RECORD);
        startService(i);
    }

    private void syncAndroidVersionWithServer() {

        if(!Constants.MY_ANDROID_VERSION(this).equals(Build.VERSION.RELEASE)) {

            Intent i = new Intent(this, LogicServerProxyService.class);
            i.setAction(LogicServerProxyService.ACTION_UPDATE_USER_RECORD);

            HashMap<DataKeys,Object> data = new HashMap<>();
            //data.put(DataKeys.ANDROID_VERSION, Build.VERSION.RELEASE);
            data.put(DataKeys.ANDROID_VERSION, Build.VERSION.RELEASE);

            i.putExtra(LogicServerProxyService.USER_RECORD, data);
            startService(i);
        }
    }

    /**
     * Saving the instance state - to be used from onPause()
     */
    private void saveInstanceState() {

        // Saving destination number
        if (_autoCompleteTextViewDestPhone != null) {
            _destPhoneNumber = _autoCompleteTextViewDestPhone.getText().toString();
            SharedPrefUtils.setString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER, _destPhoneNumber);
        }

        // Saving destination name
        if (_destTextView != null && (!_destTextView.getText().toString().isEmpty())) {
            _destName = _destTextView.getText().toString();
            SharedPrefUtils.setString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME, _destName);
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
        SharedPrefUtils.setString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER, _destPhoneNumber);


        // Saving destination name
        _destName = destName;
        SharedPrefUtils.setString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME, _destName);
    }

    private void restoreInstanceState() {

        log(Log.INFO,TAG, "Restoring instance state");

        // Restoring destination number
        String destNumber = SharedPrefUtils.getString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER);
        if (_autoCompleteTextViewDestPhone != null && destNumber != null)
            _autoCompleteTextViewDestPhone.setText(destNumber);

        // Restoring destination name
        _destName = SharedPrefUtils.getString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME);
        setDestNameTextView();
    }

    private void setDestNameTextView() {

        if( SharedPrefUtils.getBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION)) {
            YoYo.with(Techniques.StandUp)
                    .duration(1000)
                    .playOn(findViewById(R.id.destName));

        }

        if (_destPhoneNumber != null && !_destPhoneNumber.equals(""))
            _destName = ContactsUtils.getContactName(this,_destPhoneNumber);
        else
            _destName = "";

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

                    String[] nameAndPhone = ((String) av.getItemAtPosition(index)).split("\\n");
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
                            SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION, true);
                            new IsRegisteredTask(destPhone, instance).execute(getApplicationContext());

                            hideSoftKeyboardForView(_autoCompleteTextViewDestPhone); // hide keyboard so it won't bother

                        }

                    } else { // Invalid destination number

                        _destPhoneNumber = "";
                        _destName = "";


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
        y.setClass(this, BlockMCContacts.class);
        startActivity(y);
    }
    //endregion

    //region UI methods
    //region UI initializers
    private void hideSoftKeyboardForView(View view) {

        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

    }
    //endregion

    private void initializeUI() {

        setContentView(R.layout.activity_main);

        prepareMainActivityLayout();

        setCustomActionBar();
        enableHamburgerIconWithSlideMenu();
        prepareAutoCompleteTextViewDestPhoneNumber();

        prepareDestNameTextView();
        prepareDestinationEditText();
        prepareRingtoneStatus();
        prepareFetchUserProgressBar();
        prepareRingtoneNameTextView();
        prepareMediaStatusImageView();
        prepareCallNowButton();
        prepareSelectMediaButton();
        prepareSelectContactButton();
        prepareSelectProfileMediaButton();
        prepareClearTextButton();
    }

    //region UI States
    public void stateIdle() {

        enableSelectContactButton();
        enableDestinationEditText();
        disableUserFetchProgressBar();
        disableSelectProfileMediaButton();
        disableSelectCallerMediaButton();
        disableRingToneName();
        disableRingToneNameForProfile();
        disableCallButton();
        disableDestinationTextView();

        if (SharedPrefUtils.getBoolean(this,SharedPrefUtils.GENERAL,SharedPrefUtils.DISABLE_UI_ELEMENTS_ANIMATION))
                   SharedPrefUtils.setBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DISABLE_UI_ELEMENTS_ANIMATION, false);

    }

    public void stateReady() {

        enableSelectMediaButton();
        drawRingToneName();
        drawRingToneNameForProfile();
        disableUserFetchProgressBar();
        enableSelectProfileMediaButton();
        enableDestinationEditText();
        enableSelectContactButton();
        enableCallButton();
        enableSelectMediaButton();
        if (SharedPrefUtils.getBoolean(this,SharedPrefUtils.GENERAL,SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION))
                    SharedPrefUtils.setBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION, false);


    }

    public void stateDisabled() {

        reconnect();
        disableSelectCallerMediaButton();
        disableSelectProfileMediaButton();
        disableUserFetchProgressBar();
        disableSelectContactButton();
        disableDestinationEditText();
        disableCallButton();

        handleSnackBar(new SnackbarData(SnackbarData.SnackbarStatus.SHOW,
                Color.RED,
                Snackbar.LENGTH_INDEFINITE,
                getResources().getString(R.string.disconnected)));
    }

    public void stateLoading() {

        disableSelectCallerMediaButton();
        disableSelectProfileMediaButton();
        disableSelectContactButton();
        disableDestinationEditText();
        disableCallButton();
    }

    private void stateLoggedOut() {

        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private String getState() {

        return AppStateManager.getAppState(this);
    }
    //endregion

    private void syncUIwithAppState() {

        if(!AppStateManager.isLoggedIn(this))
            stateLoggedOut();

        String appState = getState();

        log(Log.INFO,TAG, "Syncing UI with appState:" + appState);

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

    //region UI elements controls
    private void prepareDestinationEditText() {

        _destinationEditText = (AutoCompleteTextView) findViewById(R.id.CallNumber);
    }

    private void prepareDestNameTextView() {

        _destTextView = (TextView) findViewById(R.id.destName);
    }

    private void prepareRingtoneStatus() {

        _ringtoneStatus = (ImageView) findViewById(R.id.ringtoneStatusArrived);
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

    private void prepareClearTextButton() {

        _clearText = (ImageButton) findViewById(R.id.clear);
        if (_clearText != null)
            _clearText.setOnClickListener(this);
    }

    private void prepareCallNowButton() {

        _callBtn = (ImageButton) findViewById(R.id.CallNow);
        if (_callBtn != null) {
            _callBtn.setOnClickListener(this);

            // to let people choose other dialers
            _callBtn.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    final Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_DIAL);
                    intent.setData(Uri.fromParts("tel", _destPhoneNumber, null));
                    startActivity(Intent.createChooser(intent, ""));


                    return true;
                }
            });
        }
    //    _callBtn_textview = (TextView) findViewById(R.id.dial_textview);
    }

    private void prepareSelectMediaButton() {

        _selectMediaBtn = (ImageButton) findViewById(R.id.selectMediaBtn);
        if (_selectMediaBtn != null)
            _selectMediaBtn.setOnClickListener(this);

        _selectMediaBtn_small = (ImageButton) findViewById(R.id.selectmedia_btn_small);
        if (_selectMediaBtn_small != null)
            _selectMediaBtn_small.setOnClickListener(this);

        _selectMediaBtn_textview = (TextView) findViewById(R.id.media_textview);
        _selectMediaBtn_textview2 = (TextView) findViewById(R.id.caller_textview2);

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

        _profile_textview = (TextView) findViewById(R.id.profile_textview);
        _profile_textview2 = (TextView) findViewById(R.id.profile_textview2);
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

        //   dataList.add(new DrawerItem(getResources().getString(R.string.media_management), R.drawable.mediaicon));
        dataList.add(new DrawerItem("", R.drawable.color_mc));
        dataList.add(new DrawerItem(getResources().getString(R.string.who_can_mc_me), R.drawable.blackwhitelist));
//        dataList.add(new DrawerItem("How To ?", R.drawable.questionmark));
//        dataList.add(new DrawerItem("Share Us", R.drawable.shareus));
//        dataList.add(new DrawerItem("Rate Us", R.drawable.rateus2));
        dataList.add(new DrawerItem(getResources().getString(R.string.app_settings), R.drawable.settingsicon));
        dataList.add(new DrawerItem(getResources().getString(R.string.about_FAQ), R.drawable.about_help));

        CustomDrawerAdapter mAdapter = new CustomDrawerAdapter(this, R.layout.custome_drawer_item,
                dataList);

        //   mAdapter = new ArrayAdapter<String>(this, R.layout.custome_drawer_item, osArray);
        _DrawerList.setAdapter(mAdapter);
    }

    private void selectNavigationItem(int position) {

        switch (position) {
            // case 0://Media Management
            //   appSettings();
            //      break;
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
                log(Log.INFO,TAG, String.valueOf(item.getItemId()));
                switch (item.getItemId()) {
                    case R.id.selectcallermedia:
                        selectMedia(ActivityRequestCodes.SELECT_CALLER_MEDIA);
                        break;
                    case R.id.previewcallermedia:

                        startPreviewStandoutWindow(SpecialMediaType.CALLER_MEDIA);

                        break;
                    case R.id.clearcallermedia:
                        ClearMediaDialog clearDialog = new ClearMediaDialog(SpecialMediaType.CALLER_MEDIA, _destPhoneNumber);
                        clearDialog.show(getFragmentManager(), TAG);
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

                log(Log.INFO,TAG, String.valueOf(item.getItemId()));

                switch (item.getItemId()) {
                    case R.id.specificprofile:
                        selectMedia(ActivityRequestCodes.SELECT_PROFILE_MEDIA);
                        break;
                    case R.id.previewprofilemedia:
                        startPreviewStandoutWindow(SpecialMediaType.PROFILE_MEDIA);
                        break;
                    case R.id.clearprofilemedia:

                        ClearMediaDialog clearDialog = new ClearMediaDialog(SpecialMediaType.PROFILE_MEDIA, _destPhoneNumber);
                        clearDialog.show(getFragmentManager(), TAG);

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

      //  _callBtn_textview.setVisibility(View.INVISIBLE);
        if (SharedPrefUtils.getBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DISABLE_UI_ELEMENTS_ANIMATION))
        {  YoYo.with(Techniques.SlideOutRight)
                .duration(300)
                .playOn(findViewById(R.id.CallNow));

        }
        else
            _callBtn.setVisibility(View.INVISIBLE);

    }

    private void enableCallButton() {

      //  _callBtn_textview.setVisibility(View.VISIBLE);
        _callBtn.setVisibility(View.VISIBLE);
        _callBtn.setEnabled(true);

        if (SharedPrefUtils.getBoolean(this,SharedPrefUtils.GENERAL,SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION))
        {
            YoYo.with(Techniques.SlideInLeft)
                    .duration(1000)
                    .playOn(findViewById(R.id.CallNow));
        }
    }


    private void disableSelectCallerMediaButton() {

        if (SharedPrefUtils.getBoolean(this,SharedPrefUtils.GENERAL,SharedPrefUtils.DISABLE_UI_ELEMENTS_ANIMATION))
        {
            Techniques tech = UI_Utils.getRandomOutTechniques();
            YoYo.with(tech)
                .duration(1000)
                .playOn(findViewById(R.id.selectMediaBtn));

            YoYo.with(tech)
                    .duration(1000)
                    .playOn(findViewById(R.id.ringtoneName));
            _selectMediaBtn.setClickable(false);
        }
        else {
            _selectMediaBtn.setVisibility(View.INVISIBLE);
            _ringToneNameTextView.setVisibility(View.INVISIBLE);
        }
        _selectMediaBtn_small.setVisibility(View.INVISIBLE);
        _selectMediaBtn_textview.setVisibility(View.INVISIBLE);
        _selectMediaBtn_textview2.setVisibility(View.INVISIBLE);
        disableMediaStatusArrived();
    }

    private void disableSelectProfileMediaButton() {

        _defaultpic_enabled.setClickable(false);
        drawSelectProfileMediaButton(false);
        _ringToneNameForProfileTextView.setVisibility(View.INVISIBLE);
        _profile_textview.setVisibility(View.INVISIBLE);
        _profile_textview2.setVisibility(View.INVISIBLE);
    }

    private void enableSelectMediaButton() {

        _selectMediaBtn.setClickable(true);
        _selectMediaBtn_small.setClickable(true);

        drawSelectMediaButton(true);
        _selectMediaBtn.setVisibility(View.VISIBLE);
        _selectMediaBtn_small.setVisibility(View.VISIBLE);
        _selectMediaBtn_textview.setVisibility(View.VISIBLE);
        _selectMediaBtn_textview2.setVisibility(View.VISIBLE);

            Techniques tech = UI_Utils.getRandomInTechniques();
            YoYo.with(tech)
                    .duration(1000)
                    .playOn(findViewById(R.id.selectMediaBtn));

            YoYo.with(tech)
                    .duration(1000)
                    .playOn(findViewById(R.id.ringtoneName));

    }

    private void enableSelectProfileMediaButton() {

        _defaultpic_enabled.setClickable(true);
        _profile_textview.setVisibility(View.VISIBLE);
        _profile_textview2.setVisibility(View.VISIBLE);
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

    private void disableDestinationTextView() {

        _destTextView.setText("");
        _destTextView.setVisibility(TextView.INVISIBLE);
    }

    private void enableDestinationTextView() {

        _destTextView.setVisibility(TextView.VISIBLE);
    }

    private void disableDestinationEditText() {

        _destinationEditText.setEnabled(false);
        _clearText.setEnabled(false);
    }

    private void enableDestinationEditText() {

        _destinationEditText.setEnabled(true);
        _clearText.setEnabled(true);
    }

    private void disableUserFetchProgressBar() {

        _fetchUserPbar.setVisibility(ProgressBar.GONE);
    }

    private void enableUserFetchProgressBar() {

        _fetchUserPbar.setVisibility(ProgressBar.VISIBLE);

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

                String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(this, _destPhoneNumber);
                if (!lastUploadedMediaPath.equals("")) {
                    fType = FileManager.getFileType(lastUploadedMediaPath);

                    BitmapUtils.execBitMapWorkerTask(_selectMediaBtn, fType, lastUploadedMediaPath, false);

                    enableMediaStatusArrived();
                    // stretch the uploaded image as it won't stretch because we use a drawable instead that we don't want to stretch
                    _selectMediaBtn.setPadding(0, 0, 0, 0);
                    _selectMediaBtn.setScaleType(ImageView.ScaleType.FIT_XY);
                    _callerHasMedia = true;
                    UI_Utils.showCaseViewAfterUploadAndCall(this, MainActivity.this);

                } else {// enabled but no uploaded media
                    String ringToneFilePath = lut_utils.getUploadedTonePerNumber(this, _destPhoneNumber);
                    if (ringToneFilePath.isEmpty())
                        UI_Utils.showCaseViewSelectMedia(this, MainActivity.this);

                    _selectMediaBtn.setImageDrawable(null);
                    _selectMediaBtn.setBackgroundColor(0x67000000);

                    _callerHasMedia = false;
                    disableMediaStatusArrived();
                }
            }

        } catch (FileInvalidFormatException |
                FileDoesNotExistException |
                FileMissingExtensionException e) {
            e.printStackTrace();
            lut_utils.removeUploadedMediaPerNumber(this, _destPhoneNumber);
        }
    }

    private void drawSelectProfileMediaButton(boolean enabled) {

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.PROFILE_MEDIA);

        try {

            if (!enabled) {
                BitmapUtils.execBitmapWorkerTask(_defaultpic_enabled, this, getResources(), R.drawable.select_profile_media_disabled, true);
            } else {

                String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(this, _destPhoneNumber);
                if (!lastUploadedMediaPath.equals("")) {
                    FileManager.FileType fType = FileManager.getFileType(lastUploadedMediaPath);

                    BitmapUtils.execBitMapWorkerTask(_defaultpic_enabled, fType, lastUploadedMediaPath, true);
                    _profileHasMedia = true;
                } else // enabled but no uploaded media
                {
                    BitmapUtils.execBitmapWorkerTask(_defaultpic_enabled, this, getResources(), R.drawable.select_profile_media_enabled, true);
                    _profileHasMedia = false;
                }
            }

        } catch (FileInvalidFormatException |
                FileDoesNotExistException |
                FileMissingExtensionException e) {
            e.printStackTrace();
            lut_utils.removeUploadedMediaPerNumber(this, _destPhoneNumber);
        }
    }

    private void drawRingToneName() {

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.CALLER_MEDIA);
        String ringToneFilePath = lut_utils.getUploadedTonePerNumber(this, _destPhoneNumber);

        try {

            if (!ringToneFilePath.isEmpty()) {
                _ringToneNameTextView.setText(FileManager.getFileNameWithExtension(ringToneFilePath));
                _ringToneNameTextView.setVisibility(View.VISIBLE);
                _callerHasRingtone = true;
                enableRingToneStatusArrived();
                UI_Utils.showCaseViewAfterUploadAndCall(this, MainActivity.this);
            } else {
                _ringToneNameTextView.setVisibility(View.INVISIBLE);
                _callerHasRingtone = false;
                disableRingToneStatusArrived();
            }
        } catch (Exception e) {
            log(Log.ERROR,TAG, "Failed to draw drawRingToneName:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

    private void drawRingToneNameForProfile() {

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.PROFILE_MEDIA);
        String ringToneFilePath = lut_utils.getUploadedTonePerNumber(this, _destPhoneNumber);

        try {

            if (!ringToneFilePath.isEmpty()) {
                _ringToneNameForProfileTextView.setText(FileManager.getFileNameWithExtension(ringToneFilePath));
                _ringToneNameForProfileTextView.setVisibility(View.VISIBLE);
                _profileHasRingtone = true;
                UI_Utils.showCaseViewAfterUploadAndCall(this, MainActivity.this);
            } else {
                _ringToneNameForProfileTextView.setVisibility(View.INVISIBLE);
                _profileHasRingtone = false;
            }
        } catch (Exception e) {
            log(Log.ERROR,TAG, "Failed to draw drawRingToneNameForProfile:" + (e.getMessage() != null ? e.getMessage() : e));
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
            _selectContactBtn.setImageResource(R.drawable.select_contact_anim);
        else
            _selectContactBtn.setImageResource(R.drawable.select_contact_disabled);
    }

    private void writeInfoSnackBar(final SnackbarData snackBarData) {

        log(Log.INFO,TAG, "Snackbar showing:" + snackBarData.getText());

        int duration = snackBarData.getDuration();
        if (duration == Snackbar.LENGTH_LONG)
            duration = 3500;

        View mainActivity = findViewById(R.id.mainActivity);

        if (mainActivity != null && snackBarData.getText() != null) {
            if(_snackBar!=null)
                _snackBar.dismiss();

           _snackBar = Snackbar
                    .make(mainActivity, Html.fromHtml(snackBarData.getText()), duration)
                    .setActionTextColor(snackBarData.getColor());
            _snackBar.setAction(R.string.snack_close, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    _snackBar.dismiss();
                }
            });

            if (snackBarData.isLoading()) {
                Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) _snackBar.getView();
                snackbarLayout.addView(new ProgressBar(this));
            }
            _snackBar.show();
        }
    }

    private void handleSnackBar(SnackbarData snackbarData) {

        switch (snackbarData.getStatus()) {
            case CLOSE:
                if(_snackBar!=null)
                    _snackBar.dismiss();
                break;

            case SHOW:
                if (snackbarData.getText() != null && !snackbarData.getText().equals(""))
                    writeInfoSnackBar(snackbarData);
                break;
        }
    }

    private void showMandatoryUpdateDialog() {

        MandatoryUpdateDialog mandatoryUpdateDialog = new MandatoryUpdateDialog();
        mandatoryUpdateDialog.show(getSupportFragmentManager(), TAG);
    }

    //endregion
    //endregion

    //region ICallbackListener methods
    @Override
    public void doCallBackAction() {

    }
    //endregion

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

    private class UploadTask extends AsyncTask<Void, Integer, Void> implements IServerProxy {
        private final String TAG = UploadTask.class.getSimpleName();
        private ConnectionToServer _connectionToServer;
        private HashMap _data;
        private ProgressDialog _progDialog;
        private UploadTask _taskInstance;
        private BufferedInputStream _bis;
        private PowerManager.WakeLock _wakeLock;
        private FileManager _fileForUpload;

        public UploadTask(Bundle bundle) {

            _fileForUpload = (FileManager) bundle.get(FILE_FOR_UPLOAD);

            HashMap<DataKeys, Object> data = new HashMap();
            String myId = Constants.MY_ID(MainActivity.this);
            double appVersion = Constants.APP_VERSION(MainActivity.this);

            data.put(DataKeys.APP_VERSION, appVersion);
            data.put(DataKeys.SOURCE_ID, myId);
            data.put(DataKeys.SOURCE_LOCALE, Locale.getDefault().getLanguage());
            data.put(DataKeys.DESTINATION_ID, bundle.get(DEST_ID));
            data.put(DataKeys.DESTINATION_CONTACT_NAME, bundle.get(DEST_NAME));
            data.put(DataKeys.MANAGED_FILE, bundle.get(FILE_FOR_UPLOAD));
            data.put(DataKeys.MD5, _fileForUpload.getMd5());
            data.put(DataKeys.EXTENSION, _fileForUpload.getFileExtension());
            data.put(DataKeys.FILE_PATH_ON_SRC_SD, _fileForUpload.getFileFullPath());
            data.put(DataKeys.FILE_SIZE, _fileForUpload.getFileSize());
            data.put(DataKeys.FILE_TYPE, _fileForUpload.getFileType());
            data.put(DataKeys.SPECIAL_MEDIA_TYPE, bundle.get(SPEC_MEDIA_TYPE));
            data.put(DataKeys.SOURCE_WITH_EXTENSION, myId + "." + _fileForUpload.getFileExtension());

            _connectionToServer = new ConnectionToServer(
                    SharedConstants.STROAGE_SERVER_HOST,
                    SharedConstants.STORAGE_SERVER_PORT,
                    this);
            _data = data;
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
            _progDialog.setMax((int)_fileForUpload.getFileSize());
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

            FileManager managedFile = (FileManager) _data.get(DataKeys.MANAGED_FILE);
            MessageToServer msgUF = new MessageToServer(ServerActionType.UPLOAD_FILE, Constants.MY_ID(MainActivity.this), _data);

            DataOutputStream dos;
            try {

                PowerManager powerManager = (PowerManager) MainActivity.this.getSystemService(Activity.POWER_SERVICE);
                _wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "UploadTask_Lock");
                _wakeLock.acquire();

                _connectionToServer.openConnection();
                _connectionToServer.sendToServer(msgUF);

                log(Log.INFO,TAG, "Initiating file data upload. [Filepath]: " + managedFile.getFileFullPath());

                dos = new DataOutputStream(_connectionToServer.getClientSocket().getOutputStream());

                FileInputStream fis = new FileInputStream(managedFile.getFile());
                _bis = new BufferedInputStream(fis);

                byte[] buf = new byte[1024 * 8];
                long bytesToRead = managedFile.getFileSize();
                int bytesRead;
                while (bytesToRead > 0 && (bytesRead = _bis.read(buf, 0, (int) Math.min(buf.length, bytesToRead))) != -1 && !isCancelled()) {
                    dos.write(buf, 0, bytesRead);
                    publishProgress((int) bytesRead);
                    bytesToRead -= bytesRead;
                }

                try {
                    Thread.sleep(1000); // Sleeping so in fast uploads the dialog won't appear and disappear too fast (like a blink)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (_progDialog != null && _progDialog.isShowing()) {
                    _progDialog.dismiss();
                }
            } catch (IOException e) {
                e.printStackTrace();
                log(Log.ERROR,TAG, "Failed:" + e.getMessage());
                BroadcastUtils.sendEventReportBroadcast(MainActivity.this, TAG,
                        new EventReport(EventType.STORAGE_ACTION_FAILURE));
            } finally {

                try {
                    _connectionToServer.closeConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(_wakeLock.isHeld())
                    _wakeLock.release();

                File tempCompressedDir = new File(Constants.TEMP_COMPRESSED_FOLDER + _destPhoneNumber);
                if(tempCompressedDir.exists()) {
                    log(Log.INFO,TAG, "Deleting " + _destPhoneNumber + "'s temp compressed folder after upload");
                    String[] entries = tempCompressedDir.list();
                    for (String s : entries) {
                        File currentFile = new File(tempCompressedDir.getPath(), s);
                        FileManager.delete(currentFile);
                    }
                }
            }

            return null;
        }

        @Override
        protected void onCancelled() {

            if (_bis != null) {
                try {
                    _bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                _connectionToServer.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }

            BroadcastUtils.sendEventReportBroadcast(MainActivity.this, TAG, new EventReport(EventType.LOADING_CANCEL, null, null));
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {

            if (_progDialog != null) {
                _progDialog.incrementProgressBy(progress[0]);
            }
        }

        @Override
        protected void onPostExecute(Void result) {

            String msg = MainActivity.this.getResources().getString(R.string.upload_success);
            // Setting state
            AppStateManager.setAppState(MainActivity.this, TAG, AppStateManager.STATE_READY);

            // Setting parameters for snackbar message
            int color = Color.GREEN;
            int sBarDuration = Snackbar.LENGTH_LONG;

            UI_Utils.showSnackBar(msg, color, sBarDuration, false, MainActivity.this);

            waitingForTransferSuccess();
        }

        @Override
        public void handleMessageFromServer(MessageToClient msg, ConnectionToServer connectionToServer) {

            try {

                ClientAction clientAction = ActionFactory.instance().getAction(msg.getActionType());
                clientAction.setConnectionToServer(connectionToServer);
                EventReport eventReport = clientAction.doClientAction(msg.getData());

                if (eventReport.status() != EventType.NO_ACTION_REQUIRED)
                    BroadcastUtils.sendEventReportBroadcast(MainActivity.this, TAG, eventReport);

            } catch (Exception e) {
                String errMsg = "Handling message from server failed. Reason:" + e.getMessage();
                log(Log.INFO,TAG, errMsg);
            } finally {

                // Finished handling request-response transaction
                try {
                    connectionToServer.closeConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void handleDisconnection(ConnectionToServer cts, String errMsg) {
            // Ignoring. We don't wait for response from server on upload anyway
        }

    }

    private void waitingForTransferSuccess() {
        if(!SharedPrefUtils.getBoolean(this,SharedPrefUtils.GENERAL,SharedPrefUtils.DONT_SHOW_AGAIN_UPLOAD_DIALOG)) {
            UI_Utils.showWaitingForTranferSuccussDialog(MainActivity.this,"MainActivity",getResources().getString(R.string.sending_to_contact)
            ,getResources().getString(R.string.waiting_for_transfer_sucess_dialog_msg));
        }
    }

    private class CompressTask extends AsyncTask<Void, Integer, Bundle> {

        private final String TAG = CompressTask.class.getSimpleName();
        private FileCompressorUtils _fileCompressor;
        private FileManager _baseFile;
        private FileManager _compressedFile;
        private CompressTask _instance = this;
        private String _destPhoneNumber;
        private Bundle _bundle;

        public CompressTask(Bundle bundle) {

            _bundle = bundle;
            _baseFile = (FileManager) bundle.get(FILE_FOR_UPLOAD);
            _destPhoneNumber = (String) bundle.get(DEST_ID);
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

                    _compressHandler.sendEmptyMessage(FileCompressorUtils.STOP_TRANSCODING_MSG);
                    _instance.cancel(true);
                }
            });

            _progDialog.show();
        }

        @Override
        protected Bundle doInBackground(Void... params) {

            Thread workerThread = new Thread(new Runnable() {

                @Override
                public void run() {

                    Log.d(TAG, "Worker started");
                    _vk = new LoadJNI();
                    PowerManager powerManager = (PowerManager) MainActivity.this.getSystemService(Activity.POWER_SERVICE);
                    PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VK_LOCK");

                    _fileCompressor = new FileCompressorUtils(_vk, wakeLock, _compressHandler);
                    _compressedFile = _fileCompressor.compressFileIfNecessary(_baseFile, _destPhoneNumber, getApplicationContext());
                    _compressHandler.sendEmptyMessage(FileCompressorUtils.FINISHED_TRANSCODING_MSG);
                }
            });

            Thread progressUpdateThread = new Thread(new Runnable() {

                ProgressCalculator pc = new ProgressCalculator(FileCompressorUtils.VK_LOG_PATH);

                @Override
                public void run() {
                    Log.d(TAG, "Progress update started");
                    int progress;
                    try {
                        _contCalcProgress = true;
                        while (_contCalcProgress) {
                            Thread.sleep(300);
                            progress = pc.calcProgress();
                            if (progress != 0 && progress < 100) {
                                log(Log.INFO,TAG, "Progress update thread. Progress is:" + progress + "%");
                                _progDialog.setProgress(progress);
                            } else if (progress == 100) {
                                log(Log.INFO,TAG, "Progress is 100, exiting progress update thread");
                                _progDialog.setProgress(100);
                                pc.initCalcParamsForNextInter();

                                // Waiting for next iteration
                                synchronized (_lock) {
                                    _lock.wait();
                                    while(!_updateThreadNextIterStarted)
                                        _lock.wait();
                                }
                            }
                        }
                    } catch (Exception e) {
                        log(Log.ERROR,TAG, e.getMessage());
                    }
                }
            });

            workerThread.start();
            progressUpdateThread.start();

            try {
                workerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (isCancelled()) {
                BroadcastUtils.sendEventReportBroadcast(MainActivity.this, TAG, new EventReport(EventType.LOADING_CANCEL, null, null));
                return null;
            }

            _bundle.putSerializable(FILE_FOR_UPLOAD, _compressedFile);
            return _bundle;
        }

        @Override
        protected void onPostExecute(Bundle bundle) {

            executeUploadTask(bundle);
        }
    }

    private class TrimTask extends AsyncTask<Void, Integer, Bundle> {

        private final String TAG = TrimTask.class.getSimpleName();
        TrimTask _instance = this;
        private FileCompressorUtils _fileCompressor;
        private FileManager _baseFile;
        private boolean calcProgress = false;
        private FileManager _trimmedFile;
        private String _destPhoneNumber;
        private Bundle _bundle;


        public TrimTask(Bundle bundle) {

            _bundle = bundle;
            _baseFile = (FileManager) _bundle.get(FILE_FOR_UPLOAD);
            _destPhoneNumber = (String) _bundle.get(DEST_ID);
        }

        @Override
        protected void onPreExecute() {

            FFMPEG_Utils ffmpeg_utils = new FFMPEG_Utils();
            if(!_baseFile.getFileType().equals(FileManager.FileType.IMAGE) &&
                    ffmpeg_utils.getFileDuration(getApplicationContext(), _baseFile) > FileCompressorUtils.MAX_DURATION) {

                calcProgress = true;

                String cancel = getResources().getString(R.string.cancel);

                _progDialog = new ProgressDialog(MainActivity.this);
                _progDialog.setIndeterminate(false);
                _progDialog.setCancelable(false);
                _progDialog.setTitle(getResources().getString(R.string.trimming));
                _progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                _progDialog.setProgress(0);
                _progDialog.setMax(100);
                _progDialog.setButton(DialogInterface.BUTTON_NEGATIVE, cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        _compressHandler.sendEmptyMessage(FileCompressorUtils.STOP_TRANSCODING_MSG);
                        _instance.cancel(true);
                    }
                });

                _progDialog.show();
            }
        }

        @Override
        protected Bundle doInBackground(Void... params) {

            Log.d(TAG, "Worker started");

            _vk = new LoadJNI();

            Thread workerThread = new Thread(new Runnable() {

                @Override
                public void run() {

                    PowerManager powerManager = (PowerManager) MainActivity.this.getSystemService(Activity.POWER_SERVICE);
                    PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VK_LOCK");

                    _fileCompressor = new FileCompressorUtils(_vk, wakeLock);
                    _trimmedFile = _fileCompressor.trimFileIfNecessary(_baseFile, _destPhoneNumber, getApplicationContext());

                    try {
                        Thread.sleep(1000); // Sleeping so in fast trimmings the dialog won't appear and disappear too fast (like a blink)
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    _compressHandler.sendEmptyMessage(FileCompressorUtils.FINISHED_TRANSCODING_MSG);

                }
            });

            Thread updateProgressThread = new Thread(new Runnable() {

                ProgressCalculator pc = new ProgressCalculator(FileCompressorUtils.VK_LOG_PATH);

                public void run() {
                    Log.d(TAG, "Progress update started");
                    int progress;
                    try {
                        while (calcProgress) {
                            Thread.sleep(300);
                            progress = pc.calcProgress();
                            if (progress != 0 && progress < 100) {
                                _progDialog.setProgress(progress);
                            } else if (progress == 100) {
                                log(Log.INFO,TAG, "Progress is 100, exiting progress update thread");
                                _progDialog.setProgress(100);
                                pc.initCalcParamsForNextInter();
                                calcProgress = false;
                            }
                        }

                    } catch (Exception e) {
                        log(Log.ERROR,TAG, e.getMessage());
                    }
                }

            });

            workerThread.start();
            updateProgressThread.start();

            try {
                workerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (isCancelled()) {
                BroadcastUtils.sendEventReportBroadcast(MainActivity.this, TAG, new EventReport(EventType.LOADING_CANCEL, null, null));
                return null;
            }

            _bundle.putSerializable(FILE_FOR_UPLOAD, _trimmedFile);
            return _bundle;
        }

        @Override
        protected void onPostExecute(Bundle bundle) {

            FileManager fileForUpload = (FileManager) bundle.get(FILE_FOR_UPLOAD);

            if(fileForUpload != null) {
                if (FileCompressorUtils.isCompressionNeeded(fileForUpload)) {
                    CompressTask compressTask = new CompressTask(bundle);
                    compressTask.execute();
                } else
                    executeUploadTask(bundle);
            }
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