package com.ui.activities;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

import com.app.AppStateManager;
import com.async_tasks.AutoCompletePopulateListAsyncTask;
import com.async_tasks.IsRegisteredTask;
import com.batch.android.Batch;
import com.crashlytics.android.Crashlytics;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.data_objects.ActivityRequestCodes;
import com.data_objects.Constants;
import com.data_objects.Contact;
import com.data_objects.KeysForBundle;
import com.data_objects.SnackbarData;
import com.flows.UploadFileFlow;
import com.interfaces.ICallbackListener;
import com.mediacallz.app.R;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.services.AbstractStandOutService;
import com.services.IncomingService;
import com.services.LogicServerProxyService;
import com.services.OutgoingService;
import com.services.PreviewService;
import com.ui.dialogs.ClearMediaDialog;
import com.ui.dialogs.InviteDialog;
import com.ui.dialogs.MandatoryUpdateDialog;
import com.utils.BitmapUtils;
import com.utils.ContactsUtils;
import com.utils.LUT_Utils;
import com.utils.MediaFileProcessingUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import DataObjects.DataKeys;
import DataObjects.SpecialMediaType;
import EventObjects.Event;
import EventObjects.EventReport;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;
import utils.PhoneNumberUtils;

import static com.crashlytics.android.Crashlytics.log;
import static com.crashlytics.android.Crashlytics.setUserIdentifier;

public class MainActivity extends AppCompatActivity implements OnClickListener, ICallbackListener {

    private final String TAG = MainActivity.class.getSimpleName();
    private String destPhoneNumber = "";
    private String destName = "";
    private boolean wentThroughOnCreate = false;
    private WebView displayYoutubeVideo;

    //region UI elements
    private ImageButton selectContactBtn;
    private ImageButton selectMediaBtn;
    private ImageButton selectMediaBtn_small;
    private TextView selectMediaBtn_textview;
    private TextView selectMediaBtn_textview2;
    private ImageButton callBtn;
    private ImageButton clearText;
    private ProgressBar fetchUserPbar;
    private BroadcastReceiver eventReceiver;
    private IntentFilter eventIntentFilter = new IntentFilter(Event.EVENT_ACTION);
    private AutoCompleteTextView autoCompleteTextViewDestPhone;
    private ListView DrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private ImageView mediaStatus;
    private ImageButton defaultpic_enabled;
    private ImageButton tutorial_imageButton;
    private TextView ringToneNameTextView;
    private TextView ringToneNameForProfileTextView;
    private TextView profile_textview;
    private TextView profile_textview2;
    private RelativeLayout mainActivityLayout;
    private ImageView ringtoneStatus;
    private AutoCompleteTextView destinationEditText;
    private TextView destTextView;
    private boolean profileHasMedia = false;
    private boolean callerHasMedia = false;
    private boolean profileHasRingtone = false;
    private boolean callerHasRingtone = false;
    private boolean openDrawer = false;
    private Snackbar snackBar;
    private Dialog tipDialog = null;
    private Dialog windowVideoDialog = null;
    private String[] tipsCircularArray;
    private int tipsNum;
    private UploadFileFlow uploadFileFlow = new UploadFileFlow();
    //endregion

    //region Activity methods (onCreate(), onPause(), onActivityResult()...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        startLoginActivityIfLoggedOut();

        // so we can know who device was crashed, and get it's phone number.
        Crashlytics.setUserIdentifier(Constants.MY_ID(getApplicationContext()));

        if (AppStateManager.didAppCrash(this)) {
            Log.w(TAG, "Detected app previously crashed. Handling...");
            AppStateManager.setAppState(this, TAG, AppStateManager.getAppPrevState(this));
            AppStateManager.setDidAppCrash(this, false);
        }

        initializeUI();
        wentThroughOnCreate = true;

        if (SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.SELECT_MEDIA_VIEW) && SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW))
            startingTipDialog();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerLayout != null)
            mDrawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        log(Log.INFO, TAG, "onStart()");

        Batch.onStart(this);

        //Copying FFMPEG license if necessary
        GeneralUtils.copyLicenseFromAssetsToSDIfNeeded(this, MediaFileProcessingUtils.workFolder);

        startLoginActivityIfLoggedOut();

        prepareEventReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        log(Log.INFO, TAG, "onResume()");
        setUserIdentifier(SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.LOGIN_NUMBER));
        openDrawer = false;
        String appState = getState();
        log(Log.INFO, TAG, "App State:" + appState);

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
            mainActivityLayout.requestFocus();

            prepareEventReceiver();

            // ASYNC TASK To Populate all contacts , it can take long time and it delays the UI
            new AutoCompletePopulateListAsyncTask(autoCompleteTextViewDestPhone).execute(getApplicationContext());

            if (!appState.equals(AppStateManager.STATE_LOADING) && !appState.equals(AppStateManager.STATE_DISABLED))
                handleSnackBar(new SnackbarData(SnackbarData.SnackbarStatus.CLOSE, 0, 0, null));

            restoreInstanceState();

            getAppRecord();

            syncAndroidVersionWithServer();

            // TODO: maybe we don't need this anymore and remove it. we have the tutorial videos and that's enough
            //startingSetWindowVideoDialog();
            UI_Utils.showCaseViewCallNumber(getApplicationContext(), MainActivity.this);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        log(Log.INFO, TAG, "onPause()");

        SharedPrefUtils.setBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION, false);
        AppStateManager.setAppInForeground(this, false);

        if (eventReceiver != null) {
            try {
                unregisterReceiver(eventReceiver);
            } catch (Exception ex) {
                log(Log.ERROR, TAG, ex.getMessage());
            }
        }
        saveInstanceState();

        UI_Utils.dismissAllStandOutWindows(getApplicationContext());

            /* Apply our splash exit (fade out) and main
            entry (fade in) animation transitions. */
       if (openDrawer)
            overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation); // open drawer animation



//        UI_Utils.unbindDrawables(findViewById(R.id.mainActivity));
//        System.gc();
    }

    @Override
    protected void onStop() {
        log(Log.INFO, TAG, "onStop()");
        Batch.onStop(this);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        log(Log.INFO, TAG, "onDestroy()");

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
                                msg);
                        writeInfoSnackBar(snackbarData);
                    } else {
                        SpecialMediaType specialMediaType = (SpecialMediaType) data.getSerializableExtra(SelectMediaActivity.RESULT_SPECIAL_MEDIA_TYPE);
                        FileManager fm = (FileManager) data.getSerializableExtra(SelectMediaActivity.RESULT_FILE);

                        Bundle bundle = new Bundle();
                        bundle.putSerializable(KeysForBundle.FILE_FOR_UPLOAD, fm);
                        bundle.putString(KeysForBundle.DEST_ID, destPhoneNumber);
                        bundle.putString(KeysForBundle.DEST_NAME, destName);
                        bundle.putSerializable(KeysForBundle.SPEC_MEDIA_TYPE, specialMediaType);

                        uploadFileFlow.executeUploadFileFlow(MainActivity.this, bundle);
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

            if (mDrawerLayout != null) {
                if (!mDrawerLayout.isDrawerOpen(GravityCompat.START))
                    mDrawerLayout.openDrawer(GravityCompat.START);
                else
                    mDrawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle != null)
            if (mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }

        return true;
    }
    //endregion (on

    //region Assisting methods (onClick(), eventReceived(), ...)
    private void startingTipDialog() {

        if (!SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_TIP)) {


            tipsCircularArray = new String[]{
                    getApplicationContext().getResources().getString(R.string.tip1_windowresize),
                    getApplicationContext().getResources().getString(R.string.tip2_profile),
                    getApplicationContext().getResources().getString(R.string.tip3_block),
                    getApplicationContext().getResources().getString(R.string.tip4_image_audio_gif),
                    getApplicationContext().getResources().getString(R.string.tip5_you_can_preview)
            };

            Random r = new Random();
            tipsNum = r.nextInt(5);

            final int arrayLength = tipsCircularArray.length;

            if (tipDialog == null) {
                tipDialog = new Dialog(MainActivity.this);

                // custom dialog

                tipDialog.setContentView(R.layout.tip_dialog);

                // set the custom dialog components - text, image and button
                final TextView text = (TextView) tipDialog.findViewById(R.id.tip_msg);
                text.setText(tipsCircularArray[tipsNum % arrayLength]);


                Button nextTipBtn = (Button) tipDialog.findViewById(R.id.next_tip);
                // if button is clicked, close the custom dialog
                nextTipBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        text.setText(tipsCircularArray[tipsNum++ % arrayLength]);
                    }
                });

                tipDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(final DialogInterface arg0) {
                        tipDialog = null;
                    }
                });

                Button skipBtn = (Button) tipDialog.findViewById(R.id.skip);
                // if button is clicked, close the custom dialog
                skipBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tipDialog.dismiss();
                    }
                });

                CheckBox checkBox = (CheckBox) tipDialog.findViewById(R.id.dont_show_tips);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        SharedPrefUtils.setBoolean(MainActivity.this, SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_TIP, isChecked);

                    }
                });
                checkBox.setText(MainActivity.this.getResources().getString(R.string.dont_show_again));


                tipDialog.show();


            }


        }
    }

    private void startingSetWindowVideoDialog() {

        Log.i(TAG, "before video dialog");
        if (!SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_WINDOW_VIDEO) && wentThroughOnCreate) {
            Log.i(TAG, "inside video dialog");
            wentThroughOnCreate = false;
            if (windowVideoDialog == null) {
                windowVideoDialog = new Dialog(MainActivity.this);

                // custom dialog
                windowVideoDialog.setContentView(R.layout.video_dialog);

                windowVideoDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(final DialogInterface arg0) {
                        windowVideoDialog = null;
                        displayYoutubeVideo.stopLoading();
                        displayYoutubeVideo.destroy();
                        Log.i(TAG, "before showcase");
                        UI_Utils.showCaseViewCallNumber(getApplicationContext(), MainActivity.this);
                    }
                });

                Button skipBtn = (Button) windowVideoDialog.findViewById(R.id.video_ok);
                // if button is clicked, close the custom dialog
                skipBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        windowVideoDialog.dismiss();
                    }
                });

                CheckBox checkBox = (CheckBox) windowVideoDialog.findViewById(R.id.video_dont_show_tips);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        SharedPrefUtils.setBoolean(MainActivity.this, SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_WINDOW_VIDEO, isChecked);
                    }
                });

                windowVideoDialog.show();

                displayYoutubeVideo = (WebView) windowVideoDialog.findViewById(R.id.set_window_video);
                displayYoutubeVideo.setVisibility(View.VISIBLE);
                String frameVideo = "<html><body>Video From YouTube<br><iframe width=\"280\" height=\"315\" src=\"https://www.youtube.com/embed/vkZE37dHErE\" frameborder=\"0\" allowfullscreen></iframe></body></html>";
                Log.i(TAG, frameVideo);
                displayYoutubeVideo.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        return false;
                    }
                });
                WebSettings webSettings = displayYoutubeVideo.getSettings();
                webSettings.setJavaScriptEnabled(true);
                displayYoutubeVideo.loadData(frameVideo, "text/html", "utf-8");

            }
        }
    }

    private void startLoginActivityIfLoggedOut() {

        if (!AppStateManager.isLoggedIn(this)) {

            stateLoggedOut();
        }
    }

//    private void startDefaultProfileMediaActivity() {
//        Intent i = new Intent(MainActivity.this, DefaultProfileMediaActivity.class);
//        startActivity(i);
//    }

    private void startPreviewStandoutWindow(SpecialMediaType specialMediaType) {

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        SharedPrefUtils.setInt(this, SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME, am.getStreamVolume(AudioManager.STREAM_MUSIC));
        log(Log.INFO, TAG, "PreviewStart MUSIC_VOLUME Original" + String.valueOf(am.getStreamVolume(AudioManager.STREAM_MUSIC)));

        // Close previous
        Intent closePrevious = new Intent(this, PreviewService.class);
        closePrevious.setAction(AbstractStandOutService.ACTION_CLOSE_ALL);
        startService(closePrevious);

        LUT_Utils lut_utils = new LUT_Utils(specialMediaType);
        Intent showPreview = new Intent(this, PreviewService.class);
        showPreview.setAction(AbstractStandOutService.ACTION_PREVIEW);
        showPreview.putExtra(AbstractStandOutService.PREVIEW_AUDIO, lut_utils.getUploadedTonePerNumber(this, destPhoneNumber));
        showPreview.putExtra(AbstractStandOutService.PREVIEW_VISUAL_MEDIA, lut_utils.getUploadedMediaPerNumber(this, destPhoneNumber));

        startService(showPreview);
    }

    private void selectMedia(int specialMediaType) {

        Intent mainIntent = new Intent(this, SelectMediaActivity.class);
        mainIntent.putExtra(SelectMediaActivity.SPECIAL_MEDIA_TYPE, specialMediaType);
        mainIntent.putExtra(SelectMediaActivity.DESTINATION_NUMBER, destPhoneNumber);
        mainIntent.putExtra(SelectMediaActivity.DESTINATION_NAME, destName);
        startActivityForResult(mainIntent, ActivityRequestCodes.SELECT_MEDIA);
        openDrawer = true;

    }

    public void onClick(View v) {

        // Saving instance state
        saveInstanceState();

        int id = v.getId();
        if (id == R.id.CallNow) {

            launchDialer(destPhoneNumber);

        } else if (id == R.id.selectMediaBtn) {
            if (callerHasMedia || callerHasRingtone)
                openCallerMediaMenu();
            else
                selectMedia(ActivityRequestCodes.SELECT_CALLER_MEDIA);

        } else if (id == R.id.selectmedia_btn_small) {
            if (callerHasMedia || callerHasRingtone)
                openCallerMediaMenu();
            else
                selectMedia(ActivityRequestCodes.SELECT_CALLER_MEDIA);

        } else if (id == R.id.selectProfileMediaBtn) {

            if (profileHasMedia || profileHasRingtone)
                openProfileMediaMenu();
            else
                selectMedia(ActivityRequestCodes.SELECT_PROFILE_MEDIA);

        } else if (id == R.id.selectContactBtn) {

            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            startActivityForResult(intent, ActivityRequestCodes.SELECT_CONTACT);

        } else if (id == R.id.clear) {

            SharedPrefUtils.setBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DISABLE_UI_ELEMENTS_ANIMATION, true);
            AutoCompleteTextView textViewToClear = (AutoCompleteTextView) findViewById(R.id.CallNumber);
            textViewToClear.setText("");

        }else if (id == R.id.tutorial_btn) {
                openMCTutorialMenu();
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
                if (!SharedPrefUtils.getBoolean(MainActivity.this, SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_CLEAR_DIALOG)) {
                    UI_Utils.showWaitingForTranferSuccussDialog(MainActivity.this, "ClearMediaDialog", getResources().getString(R.string.sending_clear_contact)
                            , getResources().getString(R.string.waiting_for_clear_transfer_sucess_dialog_msg));
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

        if (!Constants.MY_ANDROID_VERSION(this).equals(Build.VERSION.RELEASE)) {

            Intent i = new Intent(this, LogicServerProxyService.class);
            i.setAction(LogicServerProxyService.ACTION_UPDATE_USER_RECORD);

            HashMap<DataKeys, Object> data = new HashMap<>();
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
        if (autoCompleteTextViewDestPhone != null) {
            destPhoneNumber = autoCompleteTextViewDestPhone.getText().toString();
            SharedPrefUtils.setString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER, destPhoneNumber);
        }

        // Saving destination name
        if (destTextView != null && (!destTextView.getText().toString().isEmpty())) {
            destName = destTextView.getText().toString();
            SharedPrefUtils.setString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME, destName);
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
        destPhoneNumber = destNumber;
        SharedPrefUtils.setString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER, destPhoneNumber);


        // Saving destination name
        this.destName = destName;
        SharedPrefUtils.setString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME, this.destName);
    }

    private void restoreInstanceState() {

        log(Log.INFO, TAG, "Restoring instance state");

        // Restoring destination number
        String destNumber = SharedPrefUtils.getString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER);
        if (autoCompleteTextViewDestPhone != null && destNumber != null)
            autoCompleteTextViewDestPhone.setText(destNumber);

        // Restoring destination name
        destName = SharedPrefUtils.getString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME);
        setDestNameTextView();
    }

    private void setDestNameTextView() {

        if (SharedPrefUtils.getBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION)) {
            YoYo.with(Techniques.StandUp)
                    .duration(1000)
                    .playOn(findViewById(R.id.destName));

        }

        if (destPhoneNumber != null && !destPhoneNumber.equals(""))
            destName = ContactsUtils.getContactName(this, destPhoneNumber);
        else
            destName = "";

        if (destTextView != null) {

            if (destName != null && !destName.equals(""))
                destTextView.setText(destName);
            else if (destPhoneNumber != null && !destPhoneNumber.equals(""))
                destTextView.setText(destPhoneNumber);
            else {
                disableDestinationTextView();
                return;
            }

            enableDestinationTextView();
        }
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

    private void prepareAutoCompleteTextViewDestPhoneNumber() {

        final MainActivity instance = this;

        autoCompleteTextViewDestPhone = (AutoCompleteTextView) findViewById(R.id.CallNumber);
        if (autoCompleteTextViewDestPhone != null) {
            autoCompleteTextViewDestPhone.setRawInputType(InputType.TYPE_CLASS_TEXT);
            autoCompleteTextViewDestPhone.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> av, View arg1, int index, long arg3) {

                    String[] nameAndPhone = ((String) av.getItemAtPosition(index)).split("\\n");
                    String name = nameAndPhone[0];
                    String number = nameAndPhone[1];
                    String NumericNumber = PhoneNumberUtils.toValidLocalPhoneNumber(number);

                    autoCompleteTextViewDestPhone.setText(NumericNumber);
                    destName = name;
                    setDestNameTextView();
                }
            });

            autoCompleteTextViewDestPhone.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    v.setFocusable(true);
                    v.setFocusableInTouchMode(true);
                    v.performClick();
                    return false;
                }
            });

            autoCompleteTextViewDestPhone.addTextChangedListener(new TextWatcher() {

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
                            destPhoneNumber = destPhone;
                            SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION, true);
                            new IsRegisteredTask(destPhone, instance).execute(getApplicationContext());

                            hideSoftKeyboardForView(autoCompleteTextViewDestPhone); // hide keyboard so it won't bother

                        }

                    } else { // Invalid destination number

                        destPhoneNumber = "";
                        destName = "";


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

            new AutoCompletePopulateListAsyncTask(autoCompleteTextViewDestPhone).execute(getApplicationContext());
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
        prepareMCTutorialButton();
    }

    private void prepareMCTutorialButton() {

        tutorial_imageButton = (ImageButton) findViewById(R.id.tutorial_btn);
        if (tutorial_imageButton != null)
            tutorial_imageButton.setOnClickListener(this);

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

        if (SharedPrefUtils.getBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DISABLE_UI_ELEMENTS_ANIMATION))
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
        if (SharedPrefUtils.getBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION))
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

        if (!AppStateManager.isLoggedIn(this))
            stateLoggedOut();

        String appState = getState();

        log(Log.INFO, TAG, "Syncing UI with appState:" + appState);

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

        destinationEditText = (AutoCompleteTextView) findViewById(R.id.CallNumber);
    }

    private void prepareDestNameTextView() {

        destTextView = (TextView) findViewById(R.id.destName);
    }

    private void prepareRingtoneStatus() {

        ringtoneStatus = (ImageView) findViewById(R.id.ringtoneStatusArrived);
    }

    private void prepareMainActivityLayout() {

        mainActivityLayout = (RelativeLayout) findViewById(R.id.mainActivity);
    }

    private void prepareFetchUserProgressBar() {

        fetchUserPbar = (ProgressBar) findViewById(R.id.fetchuserprogress);
    }

    private void prepareMediaStatusImageView() {

        mediaStatus = (ImageView) findViewById(R.id.mediaStatusArrived);
    }

    private void prepareRingtoneNameTextView() {

        ringToneNameTextView = (TextView) findViewById(R.id.ringtoneName);
        ringToneNameForProfileTextView = (TextView) findViewById(R.id.ringtoneNameForProfile);
    }

    private void prepareClearTextButton() {

        clearText = (ImageButton) findViewById(R.id.clear);
        if (clearText != null)
            clearText.setOnClickListener(this);
    }

    //endregion

    private void prepareCallNowButton() {

        callBtn = (ImageButton) findViewById(R.id.CallNow);
        if (callBtn != null) {
            callBtn.setOnClickListener(this);

            // to let people choose other dialers
            callBtn.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    final Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_DIAL);
                    intent.setData(Uri.fromParts("tel", destPhoneNumber, null));
                    startActivity(Intent.createChooser(intent, ""));


                    return true;
                }
            });
        }
        //    _callBtn_textview = (TextView) findViewById(R.id.dial_textview);
    }

    private void prepareSelectMediaButton() {

        selectMediaBtn = (ImageButton) findViewById(R.id.selectMediaBtn);
        if (selectMediaBtn != null)
            selectMediaBtn.setOnClickListener(this);

        selectMediaBtn_small = (ImageButton) findViewById(R.id.selectmedia_btn_small);
        if (selectMediaBtn_small != null)
            selectMediaBtn_small.setOnClickListener(this);

        selectMediaBtn_textview = (TextView) findViewById(R.id.media_textview);
        selectMediaBtn_textview2 = (TextView) findViewById(R.id.caller_textview2);

    }

    private void prepareSelectContactButton() {

        selectContactBtn = (ImageButton) findViewById(R.id.selectContactBtn);
        if (selectContactBtn != null) {
            selectContactBtn.setOnClickListener(this);
        }
    }

    private void prepareSelectProfileMediaButton() {

        defaultpic_enabled = (ImageButton) findViewById(R.id.selectProfileMediaBtn);
        if (defaultpic_enabled != null)
            defaultpic_enabled.setOnClickListener(this);

        profile_textview = (TextView) findViewById(R.id.profile_textview);
        profile_textview2 = (TextView) findViewById(R.id.profile_textview2);
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

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            DrawerList = (ListView) findViewById(R.id.left_drawer);
            addDrawerItems();
            DrawerList.setOnItemClickListener(new DrawerItemClickListener());
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

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
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            mDrawerLayout.setDrawerListener(mDrawerToggle);
            mDrawerToggle.syncState();
        }
    }

    private void addDrawerItems() {

        // Add Drawer Item to dataList
        List<DrawerItem> dataList = new ArrayList<>();

        //   dataList.add(new DrawerItem(getResources().getString(R.string.media_management), R.drawable.mediaicon));
        dataList.add(new DrawerItem("", R.drawable.color_mc));
     //   dataList.add(new DrawerItem(getResources().getString(R.string.default_profile_media), R.drawable.default_profile_media));
        dataList.add(new DrawerItem(getResources().getString(R.string.who_can_mc_me), R.drawable.blackwhitelist));
//        dataList.add(new DrawerItem("How To ?", R.drawable.questionmark));
//        dataList.add(new DrawerItem("Share Us", R.drawable.shareus));
//        dataList.add(new DrawerItem("Rate Us", R.drawable.rateus2));
        dataList.add(new DrawerItem(getResources().getString(R.string.app_settings), R.drawable.settingsicon));
        dataList.add(new DrawerItem(getResources().getString(R.string.about_FAQ), R.drawable.about_help));

        CustomDrawerAdapter mAdapter = new CustomDrawerAdapter(this, R.layout.custome_drawer_item,
                dataList);

        //   mAdapter = new ArrayAdapter<String>(this, R.layout.custome_drawer_item, osArray);
        DrawerList.setAdapter(mAdapter);
    }

    private void selectNavigationItem(int position) {

        switch (position) {
            // case 0://Media Management
            //   appSettings();
            //      break;
//            case 1: // Default Profile Media
//                startDefaultProfileMediaActivity();
//                break;
            case 1: // Who Can MC me
                BlockMCContacts();
                break;
            case 2: // App Settings
                appSettings();
                break;
            case 3: // About & Help
                appAboutAndHelp();
                break;
        }

        mDrawerLayout.closeDrawer(DrawerList);
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
                log(Log.INFO, TAG, String.valueOf(item.getItemId()));
                switch (item.getItemId()) {
                    case R.id.selectcallermedia:
                        selectMedia(ActivityRequestCodes.SELECT_CALLER_MEDIA);
                        break;
                    case R.id.previewcallermedia:

                        startPreviewStandoutWindow(SpecialMediaType.CALLER_MEDIA);

                        break;
                    case R.id.clearcallermedia:
                        ClearMediaDialog clearDialog = new ClearMediaDialog(SpecialMediaType.CALLER_MEDIA, destPhoneNumber);
                        clearDialog.show(getFragmentManager(), TAG);
                        break;

                }
                return true;
            }
        });

        popup.show();

    }

    private void openMCTutorialMenu() {

        ImageButton tutorial_btn = (ImageButton) findViewById(R.id.tutorial_btn);
        //Creating the instance of PopupMenu
        PopupMenu popup = new PopupMenu(MainActivity.this, tutorial_btn);
        //Inflating the Popup using xml file
        popup.getMenuInflater().inflate(R.menu.popup_tutorial_videos, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                log(Log.INFO, TAG, String.valueOf(item.getItemId()));
                switch (item.getItemId()) {
                    case R.id.caller_media_tutorial:

                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/f0ztFdBL8Ws")));

                        break;
                    case R.id.profile_media_tutorial:

                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/f0ztFdBL8Ws")));

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

                log(Log.INFO, TAG, String.valueOf(item.getItemId()));

                switch (item.getItemId()) {
                    case R.id.specificprofile:
                        selectMedia(ActivityRequestCodes.SELECT_PROFILE_MEDIA);
                        break;
                    case R.id.previewprofilemedia:
                        startPreviewStandoutWindow(SpecialMediaType.PROFILE_MEDIA);
                        break;
                    case R.id.clearprofilemedia:

                        ClearMediaDialog clearDialog = new ClearMediaDialog(SpecialMediaType.PROFILE_MEDIA, destPhoneNumber);
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
        if (SharedPrefUtils.getBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DISABLE_UI_ELEMENTS_ANIMATION)) {
            YoYo.with(Techniques.SlideOutRight)
                    .duration(300)
                    .playOn(findViewById(R.id.CallNow));

        } else
            callBtn.setVisibility(View.INVISIBLE);

    }


    private void enableCallButton() {

        //  _callBtn_textview.setVisibility(View.VISIBLE);
        callBtn.setVisibility(View.VISIBLE);
        callBtn.setEnabled(true);

        if (SharedPrefUtils.getBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION)) {
            YoYo.with(Techniques.SlideInLeft)
                    .duration(1000)
                    .playOn(findViewById(R.id.CallNow));
        }
    }

    private void disableSelectCallerMediaButton() {

        if (SharedPrefUtils.getBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DISABLE_UI_ELEMENTS_ANIMATION)) {
            Techniques tech = UI_Utils.getRandomOutTechniques();
            YoYo.with(tech)
                    .duration(1000)
                    .playOn(findViewById(R.id.selectMediaBtn));

            YoYo.with(tech)
                    .duration(1000)
                    .playOn(findViewById(R.id.ringtoneName));
            selectMediaBtn.setClickable(false);
        } else {
            selectMediaBtn.setVisibility(View.INVISIBLE);
            ringToneNameTextView.setVisibility(View.INVISIBLE);
        }
        selectMediaBtn_small.setVisibility(View.INVISIBLE);
        selectMediaBtn_textview.setVisibility(View.INVISIBLE);
        selectMediaBtn_textview2.setVisibility(View.INVISIBLE);
        disableMediaStatusArrived();
    }

    private void disableSelectProfileMediaButton() {

        defaultpic_enabled.setClickable(false);
        drawSelectProfileMediaButton(false);
        ringToneNameForProfileTextView.setVisibility(View.INVISIBLE);
        profile_textview.setVisibility(View.INVISIBLE);
        profile_textview2.setVisibility(View.INVISIBLE);
    }

    private void enableSelectMediaButton() {

        selectMediaBtn.setClickable(true);
        selectMediaBtn_small.setClickable(true);

        drawSelectMediaButton(true);
        selectMediaBtn.setVisibility(View.VISIBLE);
        selectMediaBtn_small.setVisibility(View.VISIBLE);
        selectMediaBtn_textview.setVisibility(View.VISIBLE);
        selectMediaBtn_textview2.setVisibility(View.VISIBLE);

        Techniques tech = UI_Utils.getRandomInTechniques();
        YoYo.with(tech)
                .duration(1000)
                .playOn(findViewById(R.id.selectMediaBtn));

        YoYo.with(tech)
                .duration(1000)
                .playOn(findViewById(R.id.ringtoneName));

    }

    private void enableSelectProfileMediaButton() {

        defaultpic_enabled.setClickable(true);
        profile_textview.setVisibility(View.VISIBLE);
        profile_textview2.setVisibility(View.VISIBLE);
        drawSelectProfileMediaButton(true);
    }

    private void disableSelectContactButton() {

        selectContactBtn.setEnabled(false);
        drawSelectContactButton(false);
    }

    private void enableSelectContactButton() {

        selectContactBtn.setEnabled(true);
        drawSelectContactButton(true);
    }

    private void enableMediaStatusArrived() {

        mediaStatus.setVisibility(View.VISIBLE);
        mediaStatus.bringToFront();
    }

    private void disableMediaStatusArrived() {

        mediaStatus.setVisibility(View.INVISIBLE);
    }

    private void disableDestinationTextView() {

        destTextView.setText("");
        destTextView.setVisibility(TextView.INVISIBLE);
    }

    private void enableDestinationTextView() {

        destTextView.setVisibility(TextView.VISIBLE);
    }

    private void disableDestinationEditText() {

        destinationEditText.setEnabled(false);
        clearText.setEnabled(false);
    }

    private void enableDestinationEditText() {

        destinationEditText.setEnabled(true);
        clearText.setEnabled(true);
    }

    private void disableUserFetchProgressBar() {

        fetchUserPbar.setVisibility(ProgressBar.GONE);
    }

    private void enableUserFetchProgressBar() {

        fetchUserPbar.setVisibility(ProgressBar.VISIBLE);

    }

    private void disableRingToneStatusArrived() {

        ringtoneStatus.setVisibility(View.INVISIBLE);
    }

    private void enableRingToneStatusArrived() {

        ringtoneStatus.setVisibility(View.VISIBLE);
        ringtoneStatus.bringToFront();
    }

    private void drawSelectMediaButton(boolean enabled) {

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.CALLER_MEDIA);
        try {
            FileManager.FileType fType;

            if (!enabled)
                selectMediaBtn.setImageResource(R.drawable.select_profile_media_disabled);
            else {

                String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(this, destPhoneNumber);
                if (!lastUploadedMediaPath.equals("")) {
                    fType = FileManager.getFileType(lastUploadedMediaPath);

                    BitmapUtils.execBitMapWorkerTask(selectMediaBtn, fType, lastUploadedMediaPath, false);

                    enableMediaStatusArrived();
                    // stretch the uploaded image as it won't stretch because we use a drawable instead that we don't want to stretch
                    selectMediaBtn.setPadding(0, 0, 0, 0);
                    selectMediaBtn.setScaleType(ImageView.ScaleType.FIT_XY);
                    callerHasMedia = true;
                    UI_Utils.showCaseViewAfterUploadAndCall(this, MainActivity.this);

                } else {// enabled but no uploaded media
                    String ringToneFilePath = lut_utils.getUploadedTonePerNumber(this, destPhoneNumber);
                    if (ringToneFilePath.isEmpty())
                        UI_Utils.showCaseViewSelectMedia(this, MainActivity.this);

                    selectMediaBtn.setImageDrawable(null);
                    selectMediaBtn.setBackground(getResources().getDrawable(R.drawable.caller_media_anim));

                    callerHasMedia = false;
                    disableMediaStatusArrived();
                }
            }

        } catch (FileInvalidFormatException |
                FileDoesNotExistException |
                FileMissingExtensionException e) {
            e.printStackTrace();
            lut_utils.removeUploadedMediaPerNumber(this, destPhoneNumber);
        }
    }

    private void drawSelectProfileMediaButton(boolean enabled) {

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.PROFILE_MEDIA);

        try {

            if (!enabled) {
                BitmapUtils.execBitmapWorkerTask(defaultpic_enabled, this, getResources(), R.drawable.select_profile_media_disabled, true);
            } else {

                String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(this, destPhoneNumber);
                if (!lastUploadedMediaPath.equals("")) {
                    FileManager.FileType fType = FileManager.getFileType(lastUploadedMediaPath);

                    BitmapUtils.execBitMapWorkerTask(defaultpic_enabled, fType, lastUploadedMediaPath, true);
                    profileHasMedia = true;
                } else // enabled but no uploaded media
                {
                   // BitmapUtils.execBitmapWorkerTask(defaultpic_enabled, this, getResources(), R.drawable.select_profile_media_enabled, true);
                    defaultpic_enabled.setImageResource(R.drawable.profile_media_anim); // make the imageview pressed for PROFILE MEDIA BTN
                    profileHasMedia = false;
                }
            }

        } catch (FileInvalidFormatException |
                FileDoesNotExistException |
                FileMissingExtensionException e) {
            e.printStackTrace();
            lut_utils.removeUploadedMediaPerNumber(this, destPhoneNumber);
        }
    }

    private void drawRingToneName() {

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.CALLER_MEDIA);
        String ringToneFilePath = lut_utils.getUploadedTonePerNumber(this, destPhoneNumber);

        try {

            if (!ringToneFilePath.isEmpty()) {
                ringToneNameTextView.setText(FileManager.getFileNameWithExtension(ringToneFilePath));
                ringToneNameTextView.setVisibility(View.VISIBLE);
                callerHasRingtone = true;
                enableRingToneStatusArrived();
                UI_Utils.showCaseViewAfterUploadAndCall(this, MainActivity.this);
            } else {
                ringToneNameTextView.setVisibility(View.INVISIBLE);
                callerHasRingtone = false;
                disableRingToneStatusArrived();
            }
        } catch (Exception e) {
            log(Log.ERROR, TAG, "Failed to draw drawRingToneName:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

    private void drawRingToneNameForProfile() {

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.PROFILE_MEDIA);
        String ringToneFilePath = lut_utils.getUploadedTonePerNumber(this, destPhoneNumber);

        try {

            if (!ringToneFilePath.isEmpty()) {
                ringToneNameForProfileTextView.setText(FileManager.getFileNameWithExtension(ringToneFilePath));
                ringToneNameForProfileTextView.setVisibility(View.VISIBLE);
                profileHasRingtone = true;
                UI_Utils.showCaseViewAfterUploadAndCall(this, MainActivity.this);
            } else {
                ringToneNameForProfileTextView.setVisibility(View.INVISIBLE);
                profileHasRingtone = false;
            }
        } catch (Exception e) {
            log(Log.ERROR, TAG, "Failed to draw drawRingToneNameForProfile:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

    private void disableRingToneName() {

        ringToneNameTextView.setVisibility(View.INVISIBLE);
        disableRingToneStatusArrived();
    }

    private void disableRingToneNameForProfile() {

        ringToneNameForProfileTextView.setVisibility(View.INVISIBLE);

    }

    private void drawSelectContactButton(boolean enabled) {

        if (enabled)
            selectContactBtn.setImageResource(R.drawable.select_contact_anim);
        else
            selectContactBtn.setImageResource(R.drawable.select_contact_disabled);
    }

    private void writeInfoSnackBar(final SnackbarData snackBarData) {

        log(Log.INFO, TAG, "Snackbar showing:" + snackBarData.getText());

        int duration = snackBarData.getDuration();
        if (duration == Snackbar.LENGTH_LONG)
            duration = 3500;

        View mainActivity = findViewById(R.id.mainActivity);

        if (mainActivity != null && snackBarData.getText() != null) {
            if (snackBar != null)
                snackBar.dismiss();

            snackBar = Snackbar
                    .make(mainActivity, Html.fromHtml(snackBarData.getText()), duration)
                    .setActionTextColor(snackBarData.getColor());
            snackBar.setAction(R.string.snack_close, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackBar.dismiss();
                }
            });

            if (snackBarData.isLoading()) {
                Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackBar.getView();
                snackbarLayout.addView(new ProgressBar(this));
            }
            snackBar.show();
        }
    }

    private void handleSnackBar(SnackbarData snackbarData) {

        switch (snackbarData.getStatus()) {
            case CLOSE:
                if (snackBar != null)
                    snackBar.dismiss();
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
}


      /*  @Override     //  the menu with the 3 dots on the right, on the top action bar, to enable it uncomment this.
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }*/