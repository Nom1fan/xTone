package com.ui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.app.AppStateManager;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.data.objects.ActivityRequestCodes;
import com.data.objects.Constants;
import com.data.objects.KeysForBundle;
import com.data.objects.SnackbarData;
import com.enums.SpecialMediaType;
import com.event.Event;
import com.event.EventReport;
import com.event.EventType;
import com.files.media.MediaFile;
import com.flows.NotifySuccessPostUploadFileFlowLogic;
import com.flows.UploadFileFlow;
import com.logger.Logger;
import com.logger.LoggerFactory;
import com.mediacallz.app.R;
import com.services.AbstractStandOutService;
import com.services.PreviewService;
import com.ui.dialogs.ClearMediaDialog;
import com.utils.BitmapUtils;
import com.utils.BroadcastUtils;
import com.utils.InitUtils;
import com.utils.LUT_Utils;
import com.utils.MediaFileUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;
import com.utils.UtilityFactory;

import static com.data.objects.SnackbarData.SnackbarStatus;

/**
 * Created by rony on 28/05/17.
 */
public class DefaultMediaActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = SelectMediaActivity.class.getSimpleName();
    private Logger logger = LoggerFactory.getLogger();

    private String myPhoneNumber = "";
    private String destName = "";

    //region UI elements
    private ImageButton selectMediaBtn;
    private TextView selectMediaBtn_textview;
    private TextView selectMediaBtn_textview2;
    private ProgressBar fetchUserPbar;
    private BroadcastReceiver eventReceiver;
    private IntentFilter eventIntentFilter = new IntentFilter(Event.EVENT_ACTION);
    private ImageView mediaStatus;
    private ImageButton defaultpic_enabled;
    private TextView ringToneNameTextView;
    private TextView ringToneNameForProfileTextView;
    private TextView profile_textview;
    private TextView profile_textview2;
    private ImageView profile_arrow;
    private ImageView caller_arrow;
    private View divider1;
    private View divider2;
    private ImageView ringtoneStatus;
    private TextView destTextView;
    private boolean profileHasMedia = false;
    private boolean callerHasMedia = false;
    private boolean profileHasRingtone = false;
    private boolean callerHasRingtone = false;
    private Snackbar snackBar;
    private UploadFileFlow uploadFileFlow = new UploadFileFlow();
    private BitmapUtils bitmapUtils = UtilityFactory.instance().getUtility(BitmapUtils.class);
    private MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);


    //endregion

    //region Activity methods (onCreate(), onPause(), onActivityResult()...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        myPhoneNumber = Constants.MY_ID(getApplicationContext());
        AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_IDLE);

        initializeUI();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.custom_action_bar);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        switch (item.getItemId()) {
            case android.R.id.home:
                AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_IDLE);
                syncUIwithAppState();
                this.finish();
                return true;
        }
        return true;
    }


    @Override
    protected void onStart() {
        super.onStart();
        logger.info(TAG, "onStart()");

        prepareEventReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        logger.info(TAG, "onResume()");
        AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_READY);
        syncUIwithAppState();

        String appState = getState();
        logger.info(TAG, "App State:" + appState);

        AppStateManager.setAppInForeground(getApplicationContext(), true);


        prepareEventReceiver();

        if (!appState.equals(AppStateManager.STATE_LOADING)) {
            handleSnackBar(new SnackbarData(SnackbarStatus.CLOSE, 0, 0, null));
        }

        BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.REFRESH_UI));

    }

    @Override
    protected void onPause() {
        super.onPause();
        logger.info(TAG, "onPause()");

        SharedPrefUtils.setBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION, false);
        AppStateManager.setAppInForeground(this, false);

        if (eventReceiver != null) {
            try {
                unregisterReceiver(eventReceiver);
            } catch (Exception ex) {
                logger.error(TAG, ex.getMessage());
            }
        }

        UI_Utils.dismissAllStandOutWindows(getApplicationContext());
    }


    @Override
    protected void onDestroy() {
        logger.info(TAG, "onDestroy()");

        if (AppStateManager.getAppState(this).equals(AppStateManager.STATE_LOADING))
            AppStateManager.setAppState(this, TAG, AppStateManager.getAppPrevState(this));

        super.onDestroy();

    }

    @Override
    protected void onNewIntent(Intent intent) {

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
                        SnackbarData snackbarData = new SnackbarData(SnackbarStatus.SHOW,
                                Color.RED,
                                Snackbar.LENGTH_INDEFINITE,
                                msg);
                        writeInfoSnackBar(snackbarData);
                    } else {
                        SpecialMediaType specialMediaType = (SpecialMediaType) data.getSerializableExtra(SelectMediaActivity.RESULT_SPECIAL_MEDIA_TYPE);
                        MediaFile fm = (MediaFile) data.getSerializableExtra(SelectMediaActivity.RESULT_FILE);

                        Bundle bundle = new Bundle();
                        bundle.putSerializable(KeysForBundle.FILE_FOR_UPLOAD, fm);
                        bundle.putString(KeysForBundle.DEST_ID, myPhoneNumber);
                        bundle.putString(KeysForBundle.DEST_NAME, destName);
                        bundle.putSerializable(KeysForBundle.SPEC_MEDIA_TYPE, specialMediaType);

                        uploadFileFlow.executeUploadFileFlow(DefaultMediaActivity.this, bundle, new NotifySuccessPostUploadFileFlowLogic());

                    }
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {  // hard menu key will open and close the drawer menu also

        if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5
                && keyCode == KeyEvent.KEYCODE_BACK
                && e.getRepeatCount() == 0) {
            Log.d(TAG, "onKeyDown Called");

            AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_IDLE);
            syncUIwithAppState();

            this.finish();

            return true;
        }


        return super.onKeyDown(keyCode, e);
    }


    //endregion (on

    //region Assisting methods (onClick(), eventReceived(), ...)
    private void startPreviewStandoutWindow(SpecialMediaType specialMediaType) {

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        SharedPrefUtils.setInt(this, SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME, am.getStreamVolume(AudioManager.STREAM_MUSIC));
        logger.info(TAG, "PreviewStart MUSIC_VOLUME Original" + String.valueOf(am.getStreamVolume(AudioManager.STREAM_MUSIC)));

        // Close previous
        Intent closePrevious = new Intent(this, PreviewService.class);
        closePrevious.setAction(AbstractStandOutService.ACTION_CLOSE_ALL);
        startService(closePrevious);

        LUT_Utils lut_utils = new LUT_Utils(specialMediaType);
        Intent showPreview = new Intent(this, PreviewService.class);
        showPreview.setAction(AbstractStandOutService.ACTION_PREVIEW);
        showPreview.putExtra(AbstractStandOutService.PREVIEW_AUDIO, lut_utils.getUploadedTonePerNumber(this, myPhoneNumber));
        showPreview.putExtra(AbstractStandOutService.PREVIEW_VISUAL_MEDIA, lut_utils.getUploadedMediaPerNumber(this, myPhoneNumber));

        startService(showPreview);
    }

    private void selectMedia(SpecialMediaType specialMediaType) {

        Intent mainIntent = new Intent(this, SelectMediaActivity.class);
        mainIntent.putExtra(SelectMediaActivity.SPECIAL_MEDIA_TYPE, specialMediaType);
        mainIntent.putExtra(SelectMediaActivity.DESTINATION_NUMBER, myPhoneNumber);
        mainIntent.putExtra(SelectMediaActivity.DESTINATION_NAME, destName);
        startActivityForResult(mainIntent, ActivityRequestCodes.SELECT_MEDIA);

    }

    public void onClick(View v) {


        int id = v.getId();
        if (id == R.id.default_selectMediaBtn || id == R.id.default_callerArrow) {
            if (callerHasMedia || callerHasRingtone)
                openCallerMediaMenu();
            else
                selectMedia(SpecialMediaType.DEFAULT_CALLER_MEDIA);

        } else if (id == R.id.default_selectProfileMediaBtn || id == R.id.default_profileArrow) {

            if (profileHasMedia || profileHasRingtone)
                openProfileMediaMenu();
            else
                selectMedia(SpecialMediaType.DEFAULT_PROFILE_MEDIA);

        }
    }

    public void eventReceived(Event event) {

        final EventReport report = event.report();

        switch (report.status()) {

            case CLEAR_SENT: {
                if (!SharedPrefUtils.getBoolean(DefaultMediaActivity.this, SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_CLEAR_DIALOG)) {
                    UI_Utils.showWaitingForTranferSuccussDialog(DefaultMediaActivity.this, "ClearMediaDialog", getResources().getString(R.string.sending_clear_contact), getResources().getString(R.string.waiting_for_clear_transfer_success_dialog_msg));
                }
            }
            break;

            case REFRESH_UI: {
                SnackbarData data = (SnackbarData) report.data();
                syncUIwithAppState();

                if (data != null) {
                    handleSnackBar(data);
                }
            }
            break;

            default: // Event not meant for MainActivity receiver
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

    private void prepareTVDestinationName() {

        destTextView = (TextView) findViewById(R.id.default_destName);
    }

    //endregion

    //region UI methods

    private void initializeUI() {

        setContentView(R.layout.default_media_select_layout);

        setCustomActionBar();
        prepareTVDestinationName();
        prepareRingtoneStatus();
        prepareFetchUserProgressBar();
        prepareRingtoneNameTextView();
        prepareMediaStatusImageView();
        prepareSelectMediaButton();
        prepareSelectProfileMediaButton();
        prepareDividers();
    }


    //region UI States
    public void stateIdle() {

        disableUserFetchProgressBar();
        disableSelectProfileMediaButton();
        disableDividers();
        disableSelectCallerMediaButton();
        disableRingToneName();
        disableRingToneNameForProfile();
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
        enableDividers();
        enableSelectMediaButton();
        if (SharedPrefUtils.getBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION))
            SharedPrefUtils.setBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION, false);


    }


    public void stateLoading() {

        disableSelectCallerMediaButton();
        disableSelectProfileMediaButton();
        disableDividers();
    }


    private String getState() {

        return AppStateManager.getAppState(this);
    }

    private void syncUIwithAppState() {

        String appState = getState();

        logger.info(TAG, "Syncing UI with appState:" + appState);

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
        }
    }

    //region UI elements controls

    private void prepareRingtoneStatus() {

        ringtoneStatus = (ImageView) findViewById(R.id.default_ringtoneStatusArrived);
    }

    private void prepareFetchUserProgressBar() {

        fetchUserPbar = (ProgressBar) findViewById(R.id.default_fetchuserprogress);
    }

    private void prepareMediaStatusImageView() {

        mediaStatus = (ImageView) findViewById(R.id.default_mediaStatusArrived);
    }

    private void prepareRingtoneNameTextView() {

        ringToneNameTextView = (TextView) findViewById(R.id.default_ringtoneName);
        ringToneNameForProfileTextView = (TextView) findViewById(R.id.default_ringtoneNameForProfile);
    }

    //endregion

    private void prepareSelectMediaButton() {

        selectMediaBtn = (ImageButton) findViewById(R.id.default_selectMediaBtn);
        if (selectMediaBtn != null)
            selectMediaBtn.setOnClickListener(this);

        selectMediaBtn_textview = (TextView) findViewById(R.id.default_media_textview);
        selectMediaBtn_textview2 = (TextView) findViewById(R.id.default_caller_textview2);

        caller_arrow = (ImageView) findViewById(R.id.default_callerArrow);
        if (caller_arrow != null)
            caller_arrow.setOnClickListener(this);

    }


    private void prepareSelectProfileMediaButton() {

        defaultpic_enabled = (ImageButton) findViewById(R.id.default_selectProfileMediaBtn);
        if (defaultpic_enabled != null)
            defaultpic_enabled.setOnClickListener(this);

        profile_textview = (TextView) findViewById(R.id.default_profile_textview);
        profile_textview2 = (TextView) findViewById(R.id.default_profile_textview2);
        profile_arrow = (ImageView) findViewById(R.id.default_profileArrow);
        if (profile_arrow != null)
            profile_arrow.setOnClickListener(this);

    }


    private void prepareDividers() {
        divider1 = findViewById(R.id.default_divider1);
        divider2 = findViewById(R.id.default_divider2);
    }

    //endregion


    private void setCustomActionBar() {

        ActionBar _actionBar = getSupportActionBar();
        if (_actionBar != null) {
            _actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            _actionBar.setCustomView(R.layout.custom_action_bar);
        }
    }

    private void openCallerMediaMenu() {

        ImageButton callerMedia = (ImageButton) findViewById(R.id.default_selectMediaBtn);
        //Creating the instance of PopupMenu
        PopupMenu popup = new PopupMenu(DefaultMediaActivity.this, callerMedia);
        //Inflating the Popup using xml file
        popup.getMenuInflater().inflate(R.menu.popup_menu_default_callermedia, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                logger.info(TAG, String.valueOf(item.getItemId()));
                switch (item.getItemId()) {
                    case R.id.default_selectcallermedia:
                        selectMedia(SpecialMediaType.DEFAULT_CALLER_MEDIA);
                        break;
                    case R.id.default_previewcallermedia:

                        startPreviewStandoutWindow(SpecialMediaType.DEFAULT_CALLER_MEDIA);

                        break;
                    case R.id.default_clearcallermedia:
                        ClearMediaDialog clearDialog = new ClearMediaDialog(SpecialMediaType.DEFAULT_CALLER_MEDIA, myPhoneNumber);
                        clearDialog.show(getFragmentManager(), TAG);
                        break;

                }
                return true;
            }
        });

        popup.show();

    }

    private void openProfileMediaMenu() {
        ImageButton profile = (ImageButton) findViewById(R.id.default_selectProfileMediaBtn);
        //Creating the instance of PopupMenu
        PopupMenu popup = new PopupMenu(DefaultMediaActivity.this, profile);
        //Inflating the Popup using xml file
        popup.getMenuInflater().inflate(R.menu.popup_menu_default_profile, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {

                logger.info(TAG, String.valueOf(item.getItemId()));

                switch (item.getItemId()) {
                    case R.id.default_specificprofile:
                        selectMedia(SpecialMediaType.DEFAULT_PROFILE_MEDIA);
                        break;
                    case R.id.default_previewprofilemedia:
                        startPreviewStandoutWindow(SpecialMediaType.DEFAULT_PROFILE_MEDIA);
                        break;
                    case R.id.default_clearprofilemedia:

                        ClearMediaDialog clearDialog = new ClearMediaDialog(SpecialMediaType.DEFAULT_PROFILE_MEDIA, myPhoneNumber);
                        clearDialog.show(getFragmentManager(), TAG);

                        break;
                }
                return true;
            }
        });

        popup.show();
    }


    private void disableSelectCallerMediaButton() {

        if (SharedPrefUtils.getBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DISABLE_UI_ELEMENTS_ANIMATION)) {
            YoYo.with(Techniques.SlideOutLeft)
                    .duration(1000)
                    .playOn(findViewById(R.id.default_callerArrow));

            YoYo.with(Techniques.SlideOutLeft)
                    .duration(1000)
                    .playOn(findViewById(R.id.default_profileArrow));

            YoYo.with(Techniques.SlideOutRight)
                    .duration(1000)
                    .playOn(findViewById(R.id.default_selectProfileMediaBtn));

            YoYo.with(Techniques.SlideOutRight)
                    .duration(1000)
                    .playOn(findViewById(R.id.default_ringtoneNameForProfile));

            YoYo.with(Techniques.SlideOutRight)
                    .duration(1000)
                    .playOn(findViewById(R.id.default_selectMediaBtn));

            YoYo.with(Techniques.SlideOutRight)
                    .duration(1000)
                    .playOn(findViewById(R.id.default_ringtoneName));

            selectMediaBtn.setClickable(false);
            caller_arrow.setClickable(false);
        } else {
            selectMediaBtn.setVisibility(View.INVISIBLE);
            caller_arrow.setVisibility(View.INVISIBLE);
            profile_arrow.setVisibility(View.INVISIBLE);
            defaultpic_enabled.setVisibility(View.INVISIBLE);
            ringToneNameTextView.setVisibility(View.INVISIBLE);
            ringToneNameForProfileTextView.setVisibility(View.INVISIBLE);
            destTextView.setVisibility(View.INVISIBLE);
        }
        selectMediaBtn_textview.setVisibility(View.INVISIBLE);
        selectMediaBtn_textview2.setVisibility(View.INVISIBLE);

        disableMediaStatusArrived();
    }

    private void disableSelectProfileMediaButton() {

        defaultpic_enabled.setClickable(false);
        profile_arrow.setClickable(false);
        drawSelectProfileMediaButton(false);

        profile_textview.setVisibility(View.INVISIBLE);
        profile_textview2.setVisibility(View.INVISIBLE);

    }

    private void disableDividers() {
        divider1.setVisibility(View.INVISIBLE);
        divider2.setVisibility(View.INVISIBLE);
    }

    private void enableSelectMediaButton() {

        selectMediaBtn.setClickable(true);
        caller_arrow.setClickable(true);
        drawSelectMediaButton(true);
        selectMediaBtn.setVisibility(View.VISIBLE);
        selectMediaBtn_textview.setVisibility(View.VISIBLE);
        selectMediaBtn_textview2.setVisibility(View.VISIBLE);
        caller_arrow.setVisibility(View.VISIBLE);

        destTextView.setVisibility(View.VISIBLE);

        YoYo.with(Techniques.SlideInLeft)
                .duration(1000)
                .playOn(findViewById(R.id.default_callerArrow));

        YoYo.with(Techniques.SlideInLeft)
                .duration(1000)
                .playOn(findViewById(R.id.default_profileArrow));

        YoYo.with(Techniques.SlideInRight)
                .duration(1000)
                .playOn(findViewById(R.id.default_selectProfileMediaBtn));

        YoYo.with(Techniques.SlideInRight)
                .duration(1000)
                .playOn(findViewById(R.id.default_ringtoneNameForProfile));

        YoYo.with(Techniques.SlideInRight)
                .duration(1000)
                .playOn(findViewById(R.id.default_selectMediaBtn));

        YoYo.with(Techniques.SlideInRight)
                .duration(1000)
                .playOn(findViewById(R.id.default_ringtoneName));

    }

    private void enableSelectProfileMediaButton() {

        defaultpic_enabled.setClickable(true);
        profile_arrow.setClickable(true);
        profile_textview.setVisibility(View.VISIBLE);
        profile_textview2.setVisibility(View.VISIBLE);
        profile_arrow.setVisibility(View.VISIBLE);
        defaultpic_enabled.setVisibility(View.VISIBLE);
        drawSelectProfileMediaButton(true);
    }

    private void enableDividers() {
        divider1.setVisibility(View.VISIBLE);
        divider2.setVisibility(View.VISIBLE);
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

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.DEFAULT_CALLER_MEDIA);

        MediaFile.FileType fType;

        if (enabled) {

            String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(this, myPhoneNumber);
            if (!lastUploadedMediaPath.equals("")) {
                fType = mediaFileUtils.getFileType(lastUploadedMediaPath);

                bitmapUtils.execBitMapWorkerTask(selectMediaBtn, fType, lastUploadedMediaPath, true);

                enableMediaStatusArrived();
                // stretch the uploaded image as it won't stretch because we use a drawable instead that we don't want to stretch
                // selectMediaBtn.setPadding(0, 0, 0, 0);
                selectMediaBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
                callerHasMedia = true;

            } else {// enabled but no uploaded media
                selectMediaBtn.setImageDrawable(null);
                selectMediaBtn.setImageResource(R.drawable.profile_media_anim);

                callerHasMedia = false;
                disableMediaStatusArrived();
            }
        }
    }

    private void drawSelectProfileMediaButton(boolean enabled) {

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.DEFAULT_PROFILE_MEDIA);

        if (enabled) {

            String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(this, myPhoneNumber);
            if (!lastUploadedMediaPath.equals("")) {
                MediaFile.FileType fType = mediaFileUtils.getFileType(lastUploadedMediaPath);

                bitmapUtils.execBitMapWorkerTask(defaultpic_enabled, fType, lastUploadedMediaPath, true);
                profileHasMedia = true;
            } else // enabled but no uploaded media
            {
                // BitmapUtilsImpl.execBitmapWorkerTask(defaultpic_enabled, this, getResources(), R.drawable.select_profile_media_enabled, true);
                defaultpic_enabled.setImageResource(R.drawable.mc_caller_media_anim); // make the imageview pressed for PROFILE MEDIA BTN
                profileHasMedia = false;
            }
        }
    }

    private void drawRingToneName() {

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.DEFAULT_CALLER_MEDIA);
        String ringToneFilePath = lut_utils.getUploadedTonePerNumber(this, myPhoneNumber);

        try {

            if (!ringToneFilePath.isEmpty()) {
                ringToneNameTextView.setText(mediaFileUtils.getFileNameWithExtension(ringToneFilePath));
                ringToneNameTextView.setVisibility(View.VISIBLE);
                callerHasRingtone = true;
                enableRingToneStatusArrived();
            } else {
                ringToneNameTextView.setVisibility(View.INVISIBLE);
                callerHasRingtone = false;
                disableRingToneStatusArrived();
            }
        } catch (Exception e) {
            logger.error(TAG, "Failed to draw drawRingToneName:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

    private void drawRingToneNameForProfile() {

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.DEFAULT_PROFILE_MEDIA);
        String ringToneFilePath = lut_utils.getUploadedTonePerNumber(this, myPhoneNumber);

        try {

            if (!ringToneFilePath.isEmpty()) {
                ringToneNameForProfileTextView.setText(mediaFileUtils.getFileNameWithExtension(ringToneFilePath));
                ringToneNameForProfileTextView.setVisibility(View.VISIBLE);
                profileHasRingtone = true;
            } else {
                ringToneNameForProfileTextView.setVisibility(View.INVISIBLE);
                profileHasRingtone = false;
            }
        } catch (Exception e) {
            logger.error(TAG, "Failed to draw drawRingToneNameForProfile:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

    private void disableRingToneName() {

        ringToneNameTextView.setVisibility(View.INVISIBLE);
        disableRingToneStatusArrived();
    }

    private void disableRingToneNameForProfile() {

        ringToneNameForProfileTextView.setVisibility(View.INVISIBLE);

    }


    private void writeInfoSnackBar(final SnackbarData snackBarData) {

        logger.info(TAG, "Snackbar showing:" + snackBarData.getText());

        int duration = snackBarData.getDuration();
        if (duration == Snackbar.LENGTH_LONG)
            duration = 3500;

        View mainActivity = findViewById(R.id.default_mainActivity);

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

    private void disableSnackbar() {
        handleSnackBar(new SnackbarData(SnackbarStatus.CLOSE));
    }


    //endregion


}
