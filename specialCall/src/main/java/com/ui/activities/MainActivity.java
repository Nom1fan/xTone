package com.ui.activities;

import android.app.Dialog;
import android.app.SearchManager;
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
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.app.AppStateManager;
import com.async.tasks.IsRegisteredTask;
import com.async.tasks.SendBugEmailAsyncTask;
import com.batch.android.Batch;
import com.crashlytics.android.Crashlytics;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.data.objects.ActivityRequestCodes;
import com.data.objects.Constants;
import com.data.objects.ContactWrapper;
import com.data.objects.KeysForBundle;
import com.data.objects.SnackbarData;
import com.enums.SpecialMediaType;
import com.enums.UserStatus;
import com.event.Event;
import com.event.EventReport;
import com.exceptions.FileDoesNotExistException;
import com.exceptions.FileInvalidFormatException;
import com.exceptions.FileMissingExtensionException;
import com.files.media.MediaFile;
import com.flows.UploadFileFlow;
import com.interfaces.ICallbackListener;
import com.mediacallz.app.R;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.services.AbstractStandOutService;
import com.services.IncomingService;
import com.services.OutgoingService;
import com.services.PreviewService;
import com.services.ServerProxyService;
import com.ui.dialogs.ClearMediaDialog;
import com.ui.dialogs.InviteDialog;
import com.ui.dialogs.MandatoryUpdateDialog;
import com.utils.BitmapUtils;
import com.utils.CacheUtils;
import com.utils.ContactsUtils;
import com.utils.LUT_Utils;
import com.utils.MediaFileProcessingUtils;
import com.utils.MediaFilesUtils;
import com.utils.PhoneNumberUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.crashlytics.android.Crashlytics.log;
import static com.crashlytics.android.Crashlytics.setUserIdentifier;
import static com.data.objects.SnackbarData.SnackbarStatus;

public class MainActivity extends AppCompatActivity implements OnClickListener, ICallbackListener {

    private final String TAG = MainActivity.class.getSimpleName();
    private String destPhoneNumber = "";
    private String destName = "";
    private boolean wentThroughOnCreate = false;
    private WebView displayYoutubeVideo;

    //region UI elements
    private ImageButton selectMediaBtn;
    private TextView selectMediaBtn_textview;
    private TextView selectMediaBtn_textview2;
    private ImageButton callBtn;
    private ImageButton clearText;
    private ProgressBar fetchUserPbar;
    private BroadcastReceiver eventReceiver;
    private IntentFilter eventIntentFilter = new IntentFilter(Event.EVENT_ACTION);
    private TextView TVDestinationPhone;
    private ListView drawerList;
    private ListView contactsListView;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private ImageView mediaStatus;
    private ImageButton defaultpic_enabled;
    private ImageButton tutorial_imageButton;
    private TextView ringToneNameTextView;
    private TextView ringToneNameForProfileTextView;
    private TextView profile_textview;
    private TextView profile_textview2;
    private ImageView profile_arrow;
    private ImageView caller_arrow;
    private View divider1;
    private View divider2;
    private RelativeLayout mainActivityLayout;
    private ImageView ringtoneStatus;
    private TextView destTextView;
    private boolean profileHasMedia = false;
    private boolean callerHasMedia = false;
    private boolean profileHasRingtone = false;
    private boolean callerHasRingtone = false;
    private boolean openDrawer = false;
    private Snackbar snackBar;
    private Dialog windowVideoDialog = null;
    private UploadFileFlow uploadFileFlow = new UploadFileFlow();
    private List<ContactWrapper> arrayOfUsers;
    private OnlineContactAdapter adapter;
    private SearchView searchView;
    //endregion

    //region Activity methods (onCreate(), onPause(), onActivityResult()...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        startLoginActivityIfLoggedOut();

        // so we can know who device was crashed, and get it's phone number.
        Crashlytics.setUserIdentifier(Constants.MY_ID(getApplicationContext()));

        if (AppStateManager.isLoggedIn(this)) // should always start from idle and registeredContactLV
            AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_IDLE);


        if (AppStateManager.didAppCrash(this)) {
            Log.w(TAG, "Detected app previously crashed. Handling...");
            AppStateManager.setAppState(this, TAG, AppStateManager.getAppPrevState(this));
            AppStateManager.setDidAppCrash(this, false);
        }

        initializeUI();
        wentThroughOnCreate = true;


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

            if (!appState.equals(AppStateManager.STATE_LOADING))
                handleSnackBar(new SnackbarData(SnackbarStatus.CLOSE, 0, 0, null));

            restoreInstanceState();

            getAppRecord();

            syncAndroidVersionWithServer();

            if (TVDestinationPhone.getText().toString().isEmpty() && AppStateManager.isLoggedIn(this)) {
                ServerProxyService.getRegisteredContacts(getApplicationContext());
                AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_IDLE);
                syncUIwithAppState();
            }

            UI_Utils.showCaseViewCallNumber(getApplicationContext(), MainActivity.this);

            if (SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.SELECT_MEDIA_VIEW) && SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW) && wentThroughOnCreate)
                startingTipDialog();

        }
    }

    @Override
    public void onBackPressed() {
        Log.d("CDA", "onBackPressed Called");
//        clearText.performClick();

        if (TVDestinationPhone != null) {
            TVDestinationPhone.setText("");
            if (destTextView != null)
                destTextView.setText("");
        }

        AppStateManager.setAppState(this, TAG, AppStateManager.STATE_IDLE);
        UI_Utils.refreshUI(this, new SnackbarData(SnackbarStatus.CLOSE));
    }

    public class OnlineContactAdapter extends ArrayAdapter<ContactWrapper> implements Filterable {
        private List<ContactWrapper> allContacts;

        private List<ContactWrapper> dynamicContacts;


        OnlineContactAdapter(Context context, List<ContactWrapper> allContacts) {
            super(context, 0, allContacts);
            this.allContacts = allContacts;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    constraint = constraint.toString().toLowerCase();
                    FilterResults result = new FilterResults();
                    if (constraint.toString().length() > 0) {
                        List<ContactWrapper> founded = new ArrayList<>();
                        dynamicContacts = new ArrayList<>(arrayOfUsers);
                        for (ContactWrapper contactWrapper : dynamicContacts) {
                            if (contactWrapper.getContact().getName().toLowerCase().contains(constraint) || contactWrapper.getContact().getPhoneNumber().contains(constraint)) {
                                founded.add(contactWrapper);
                            }
                        }
                        result.values = founded;
                        result.count = founded.size();
                    } else {
                        result.values = dynamicContacts;
                        if (dynamicContacts != null)
                            result.count = dynamicContacts.size();
                    }
                    return result;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clear();
                    if (results.values != null)
                        for (ContactWrapper contactWrapper : (List<ContactWrapper>) results.values) {
                            add(contactWrapper);
                        }
                    notifyDataSetChanged();
                }
            };
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // Get the data item for this position
            ContactWrapper contactWrapper = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.online_contact_row, parent, false);
            }
            // Lookup view for data population
            TextView tvName = (TextView) convertView.findViewById(R.id.contact_name);
            TextView tvPhone = (TextView) convertView.findViewById(R.id.contact_phone);
            ImageView contactStatusImage = (ImageView) convertView.findViewById(R.id.contact_status);
            // Populate the data into the template view using the data object
            tvName.setText(contactWrapper != null ? contactWrapper.getContact().getName() : null);
            tvPhone.setText(contactWrapper != null ? contactWrapper.getContact().getPhoneNumber() : null);

            if (contactWrapper != null && contactWrapper.getUserStatus().equals(UserStatus.REGISTERED)) {
                contactStatusImage.setImageResource(android.R.drawable.presence_online);
                contactStatusImage.setTag("on");
            } else {
                contactStatusImage.setImageResource(android.R.drawable.presence_invisible);
                contactStatusImage.setTag("off");
            }
            // Return the completed view to render on screen
            return convertView;
        }
    }

    @Override // add search functionality
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // Inflate menu to add items to action bar if it is present.
        inflater.inflate(R.menu.select_contact_menu, menu);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        return true;
    }

    // pass the search keyword to filter from the adapter
    private SearchView.OnQueryTextListener onQueryTextListener() {
        return new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                String ifOnlyPhoneNumber = PhoneNumberUtils.toNumeric(s);
                if (PhoneNumberUtils.isValidPhoneNumber(ifOnlyPhoneNumber)) {
                    TVDestinationPhone.setText(ifOnlyPhoneNumber);
                    destTextView.setText(ifOnlyPhoneNumber);

                    new IsRegisteredTask(ifOnlyPhoneNumber, MainActivity.this).execute(getApplicationContext());

                    return false;
                }
                adapter.getFilter().filter(searchView.getQuery());
                return false;
            }
        };
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

        wentThroughOnCreate=false;
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

        clearText.performClick();
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
                        bundle.putString(KeysForBundle.DEST_ID, destPhoneNumber);
                        bundle.putString(KeysForBundle.DEST_NAME, destName);
                        bundle.putSerializable(KeysForBundle.SPEC_MEDIA_TYPE, specialMediaType);

                        uploadFileFlow.executeUploadFileFlow(MainActivity.this, bundle);

                    }
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
        if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5
                && keyCode == KeyEvent.KEYCODE_BACK
                && e.getRepeatCount() == 0) {
            Log.d("CDA", "onKeyDown Called");

            if (mDrawerLayout != null) {
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
            }

            onBackPressed();
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
            Intent tip = new Intent(getApplicationContext(), TipActivityDialog.class);
            startActivity(tip);
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

    private void selectMedia(SpecialMediaType specialMediaType) {

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

        } else if (id == R.id.selectMediaBtn || id == R.id.callerArrow) {
            if (callerHasMedia || callerHasRingtone)
                openCallerMediaMenu();
            else
                selectMedia(SpecialMediaType.CALLER_MEDIA);

        } else if (id == R.id.selectProfileMediaBtn || id == R.id.profileArrow) {

            if (profileHasMedia || profileHasRingtone)
                openProfileMediaMenu();
            else
                selectMedia(SpecialMediaType.PROFILE_MEDIA);

        } else if (id == R.id.clear) {

            if (TVDestinationPhone != null) {
                TVDestinationPhone.setText("");
                if (destTextView != null)
                    destTextView.setText("");
            }

            AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_IDLE);
            syncUIwithAppState();

        } else if (id == R.id.tutorial_btn) {
            openMCTutorialMenu();
        }
    }

    public void eventReceived(Event event) {

        final EventReport report = event.report();

        switch (report.status()) {

            case APP_RECORD_RECEIVED: {
                double lastSupportedVersion = (double) report.data();

                if (Constants.APP_VERSION() < lastSupportedVersion)
                    showMandatoryUpdateDialog();
            }
            break;

            case USER_REGISTERED_FALSE:
                enableInviteForUnregisteredUserFunctionality("");
                break;

            case USER_REGISTERED_TRUE:
                enableUserRegisterFunctionality();
                break;

            case CLEAR_SENT:
                if (!SharedPrefUtils.getBoolean(MainActivity.this, SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_CLEAR_DIALOG)) {
                    UI_Utils.showWaitingForTranferSuccussDialog(MainActivity.this, "ClearMediaDialog", getResources().getString(R.string.sending_clear_contact), getResources().getString(R.string.waiting_for_clear_transfer_success_dialog_msg));
                }

                break;

            case REFRESH_UI:
                SnackbarData data = (SnackbarData) report.data();
                syncUIwithAppState();

                if (data != null)
                    handleSnackBar(data);
                break;

            case GET_REGISTERED_CONTACTS_SUCCESS:
                // Construct the data source

                // Create the adapter to convert the array to views
                arrayOfUsers = new ArrayList<>((List<ContactWrapper>) event.report().data());
                adapter = new OnlineContactAdapter(this, (List<ContactWrapper>) event.report().data());
                // Attach the adapter to a ListView
                contactsListView.setAdapter(adapter);
                contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        destPhoneNumber = ((TextView) view.findViewById(R.id.contact_phone)).getText().toString();
                        String status_tag =  String.valueOf(((ImageView) view.findViewById(R.id.contact_status)).getTag());
                        destName = ((TextView) view.findViewById(R.id.contact_name)).getText().toString();

                        SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION, true);

                        if (status_tag.equals("on")){
                                enableUserRegisterFunctionality();
                                TVDestinationPhone.setText(destPhoneNumber);
                                destTextView.setText(destName);
                                setDestNameTextView();
                            } else {
                                enableInviteForUnregisteredUserFunctionality(destName);
                            }

                    }
                });
                // Associate searchable configuration with the SearchView
                SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
                searchView.setOnQueryTextListener(onQueryTextListener());
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
                adapter.notifyDataSetChanged();
                syncUIwithAppState();
                break;

            default: // Event not meant for MainActivity receiver
        }
    }

    private void enableUserRegisterFunctionality() {
        AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_READY);
        syncUIwithAppState();
        String msg = String.format(getApplicationContext().getResources().getString(R.string.user_is_registered),
                ContactsUtils.getContactNameHtml(getApplicationContext(), destPhoneNumber));
        CacheUtils.setPhone(getApplicationContext(), destPhoneNumber);

        UI_Utils.showSnackBar(msg, Color.GREEN, Snackbar.LENGTH_LONG, false, getApplicationContext());

    }

    private void enableInviteForUnregisteredUserFunctionality(String name) {
        InviteDialog inviteDialog = new InviteDialog(name);
        inviteDialog.show(getFragmentManager(), TAG);
    }

    //TODO change this to campaign API push for all users in case of last supported version change
    private void getAppRecord() {

        Intent i = new Intent(this, ServerProxyService.class);
        i.setAction(ServerProxyService.ACTION_GET_APP_RECORD);
        startService(i);
    }

    private void syncAndroidVersionWithServer() {
        if (!Constants.MY_ANDROID_VERSION(this).equals(Build.VERSION.RELEASE)) {
            Intent i = new Intent(this, ServerProxyService.class);
            i.setAction(ServerProxyService.ACTION_UPDATE_USER_RECORD);
            startService(i);
        }
    }

    /**
     * Saving the instance state - to be used from onPause()
     */
    private void saveInstanceState() {

        // Saving destination number
        if (TVDestinationPhone != null) {
            destPhoneNumber = TVDestinationPhone.getText().toString();
            SharedPrefUtils.setString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER, destPhoneNumber);
        }

        // Saving destination name
        String textview_name = destTextView.getText().toString();
        if (destTextView != null && (!textview_name.isEmpty())) {
            destName = textview_name;
            SharedPrefUtils.setString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME, destName);
        }
    }


    private void restoreInstanceState() {

        log(Log.INFO, TAG, "Restoring instance state");

        // Restoring destination number
        String destNumber = SharedPrefUtils.getString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER);
        if (TVDestinationPhone != null && destNumber != null) {
            TVDestinationPhone.setText(destNumber);

            // Restoring destination name
            destName = SharedPrefUtils.getString(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME);

            setDestNameTextView();
        }
    }

    private void setDestNameTextView() {

        if (SharedPrefUtils.getBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION)) {
            YoYo.with(Techniques.StandUp)
                    .duration(1000)
                    .playOn(findViewById(R.id.destName));

        }

        if (destTextView != null) {

            if (destName != null && !destName.equals("") && !TVDestinationPhone.getText().equals(""))
                destTextView.setText(destName);
            else {
                disableDestinationTextView();
                return;
            }

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

    private void prepareTVDestinationPhoneNumber() {

        TVDestinationPhone = (TextView) findViewById(R.id.CallNumber);
        destTextView = (TextView) findViewById(R.id.destName);
    }

    private void BlockMCContacts() {
        saveInstanceState();
        Intent y = new Intent();
        y.setClass(this, BlockMCContacts.class);
        startActivity(y);
    }
    //endregion

    //region UI methods

    private void initializeUI() {

        setContentView(R.layout.activity_main);

        prepareMainActivityLayout();

        setCustomActionBar();
        enableHamburgerIconWithSlideMenu();
        prepareTVDestinationPhoneNumber();
        prepareRingtoneStatus();
        prepareFetchUserProgressBar();
        prepareRingtoneNameTextView();
        prepareMediaStatusImageView();
        prepareCallNowButton();
        prepareSelectMediaButton();
        prepareSelectProfileMediaButton();
        prepareDividers();
        prepareClearTextButton();
        prepareMCTutorialButton();
        prepareStartingView();
    }

    private void prepareStartingView() {

        contactsListView = (ListView) findViewById(R.id.online_contacts);

    }

    private void prepareMCTutorialButton() {

        tutorial_imageButton = (ImageButton) findViewById(R.id.tutorial_btn);
        if (tutorial_imageButton != null)
            tutorial_imageButton.setOnClickListener(this);

    }

    private void enableContactsListView() {
        if (AppStateManager.isLoggedIn(this)) {
            if (contactsListView != null) {
                ListAdapter adapter = contactsListView.getAdapter();
                if (adapter != null && adapter.getCount() > 0) {
                    contactsListView.setVisibility(View.VISIBLE);
                } else {
                    ServerProxyService.getRegisteredContacts(this);
                }
            } else {
                ServerProxyService.getRegisteredContacts(this);
            }
        }
    }

    //region UI States
    public void stateIdle() {

        disableUserFetchProgressBar();
        disableSelectProfileMediaButton();
        disableDividers();
        disableSelectCallerMediaButton();
        disableRingToneName();
        disableRingToneNameForProfile();
        disableCallButton();
        disableDestinationTextView();
        enableStartingViews();

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
        enableCallButton();
        enableSelectMediaButton();
        disableStartingViews();
        if (SharedPrefUtils.getBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION))
            SharedPrefUtils.setBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION, false);


    }

    private void disableStartingViews() {

        contactsListView.setVisibility(View.INVISIBLE);

        if (searchView != null) {
            searchView.setVisibility(View.GONE);
        }



    }

    private void enableStartingViews() {

        enableContactsListView();

        if (searchView != null) {
            searchView.setVisibility(View.VISIBLE);
        }

        YoYo.with(Techniques.FadeIn)
                .duration(1000)
                .playOn(findViewById(R.id.online_contacts));

    }

    public void stateLoading() {

        disableSelectCallerMediaButton();
        disableSelectProfileMediaButton();
        disableDividers();
        disableCallButton();
        disableStartingViews();
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
        }
    }

    //region UI elements controls

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
    }

    private void prepareSelectMediaButton() {

        selectMediaBtn = (ImageButton) findViewById(R.id.selectMediaBtn);
        if (selectMediaBtn != null)
            selectMediaBtn.setOnClickListener(this);

        selectMediaBtn_textview = (TextView) findViewById(R.id.media_textview);
        selectMediaBtn_textview2 = (TextView) findViewById(R.id.caller_textview2);

        caller_arrow = (ImageView) findViewById(R.id.callerArrow);
        if (caller_arrow != null)
            caller_arrow.setOnClickListener(this);

    }



    private void prepareSelectProfileMediaButton() {

        defaultpic_enabled = (ImageButton) findViewById(R.id.selectProfileMediaBtn);
        if (defaultpic_enabled != null)
            defaultpic_enabled.setOnClickListener(this);

        profile_textview = (TextView) findViewById(R.id.profile_textview);
        profile_textview2 = (TextView) findViewById(R.id.profile_textview2);
        profile_arrow = (ImageView) findViewById(R.id.profileArrow);
        if (profile_arrow != null)
            profile_arrow.setOnClickListener(this);

    }


    private void prepareDividers() {
        divider1 = (View) findViewById(R.id.divider1);
        divider2 = (View) findViewById(R.id.divider2);
    }

    //endregion


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
            drawerList = (ListView) findViewById(R.id.left_drawer);
            addDrawerItems();
            drawerList.setOnItemClickListener(new DrawerItemClickListener());
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
        dataList.add(new DrawerItem(getResources().getString(R.string.report_bug), R.drawable.report_bug));

        CustomDrawerAdapter mAdapter = new CustomDrawerAdapter(this, R.layout.custome_drawer_item,
                dataList);

        //   mAdapter = new ArrayAdapter<String>(this, R.layout.custome_drawer_item, osArray);
        drawerList.setAdapter(mAdapter);
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
            case 4: // Send Bug
                SendBugEmailAsyncTask sendBugEmailAsyncTask = new SendBugEmailAsyncTask(this);
                sendBugEmailAsyncTask.execute();
                break;
        }

        mDrawerLayout.closeDrawer(drawerList);
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
                        selectMedia(SpecialMediaType.CALLER_MEDIA);
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

                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/w2Jci6Ujbp0")));

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
                        selectMedia(SpecialMediaType.PROFILE_MEDIA);
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

        if (SharedPrefUtils.getBoolean(this, SharedPrefUtils.GENERAL, SharedPrefUtils.DISABLE_UI_ELEMENTS_ANIMATION)) {
            YoYo.with(Techniques.SlideOutLeft)
                    .duration(300)
                    .playOn(findViewById(R.id.CallNow));

        } else
            callBtn.setVisibility(View.INVISIBLE);

    }


    private void enableCallButton() {

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
            YoYo.with(Techniques.SlideOutLeft)
                    .duration(1000)
                    .playOn(findViewById(R.id.callerArrow));

            YoYo.with(Techniques.SlideOutLeft)
                    .duration(1000)
                    .playOn(findViewById(R.id.profileArrow));

            YoYo.with(Techniques.SlideOutRight)
                    .duration(1000)
                    .playOn(findViewById(R.id.selectProfileMediaBtn));

            YoYo.with(Techniques.SlideOutRight)
                    .duration(1000)
                    .playOn(findViewById(R.id.ringtoneNameForProfile));

            YoYo.with(Techniques.SlideOutRight)
                    .duration(1000)
                    .playOn(findViewById(R.id.selectMediaBtn));

            YoYo.with(Techniques.SlideOutRight)
                    .duration(1000)
                    .playOn(findViewById(R.id.ringtoneName));

            selectMediaBtn.setClickable(false);
            caller_arrow.setClickable(false);
        } else {
            selectMediaBtn.setVisibility(View.INVISIBLE);
            caller_arrow.setVisibility(View.INVISIBLE);
            profile_arrow.setVisibility(View.INVISIBLE);
            defaultpic_enabled.setVisibility(View.INVISIBLE);
            ringToneNameTextView.setVisibility(View.INVISIBLE);
            ringToneNameForProfileTextView.setVisibility(View.INVISIBLE);
            TVDestinationPhone.setVisibility(View.INVISIBLE);
            clearText.setVisibility(View.INVISIBLE);
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

        TVDestinationPhone.setVisibility(View.VISIBLE);
        destTextView.setVisibility(View.VISIBLE);
        clearText.setVisibility(View.VISIBLE);
        TVDestinationPhone.setEnabled(true);
        clearText.setEnabled(true);

        YoYo.with(Techniques.SlideInLeft)
                .duration(1000)
                .playOn(findViewById(R.id.callerArrow));

        YoYo.with(Techniques.SlideInLeft)
                .duration(1000)
                .playOn(findViewById(R.id.profileArrow));

        YoYo.with(Techniques.SlideInRight)
                .duration(1000)
                .playOn(findViewById(R.id.selectProfileMediaBtn));

        YoYo.with(Techniques.SlideInRight)
                .duration(1000)
                .playOn(findViewById(R.id.ringtoneNameForProfile));

        YoYo.with(Techniques.SlideInRight)
                .duration(1000)
                .playOn(findViewById(R.id.selectMediaBtn));

        YoYo.with(Techniques.SlideInRight)
                .duration(1000)
                .playOn(findViewById(R.id.ringtoneName));

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

        LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.CALLER_MEDIA);
        try {
            MediaFile.FileType fType;

            if (enabled) {

                String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(this, destPhoneNumber);
                if (!lastUploadedMediaPath.equals("")) {
                    fType = MediaFilesUtils.getFileType(lastUploadedMediaPath);

                    BitmapUtils.execBitMapWorkerTask(selectMediaBtn, fType, lastUploadedMediaPath, true);

                    enableMediaStatusArrived();
                    // stretch the uploaded image as it won't stretch because we use a drawable instead that we don't want to stretch
                    // selectMediaBtn.setPadding(0, 0, 0, 0);
                    selectMediaBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    callerHasMedia = true;
                    UI_Utils.showCaseViewAfterUploadAndCall(this, MainActivity.this);

                } else {// enabled but no uploaded media
                    String ringToneFilePath = lut_utils.getUploadedTonePerNumber(this, destPhoneNumber);
                    if (ringToneFilePath.isEmpty())
                        UI_Utils.showCaseViewSelectMedia(this, MainActivity.this);

                    selectMediaBtn.setImageDrawable(null);
                    selectMediaBtn.setImageResource(R.drawable.profile_media_anim);

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

            if (enabled) {

                String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(this, destPhoneNumber);
                if (!lastUploadedMediaPath.equals("")) {
                    MediaFile.FileType fType = MediaFilesUtils.getFileType(lastUploadedMediaPath);

                    BitmapUtils.execBitMapWorkerTask(defaultpic_enabled, fType, lastUploadedMediaPath, true);
                    profileHasMedia = true;
                } else // enabled but no uploaded media
                {
                    // BitmapUtils.execBitmapWorkerTask(defaultpic_enabled, this, getResources(), R.drawable.select_profile_media_enabled, true);
                    defaultpic_enabled.setImageResource(R.drawable.mc_caller_media_anim); // make the imageview pressed for PROFILE MEDIA BTN
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
                ringToneNameTextView.setText(MediaFilesUtils.getFileNameWithExtension(ringToneFilePath));
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
                ringToneNameForProfileTextView.setText(MediaFilesUtils.getFileNameWithExtension(ringToneFilePath));
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

    private void disableSnackbar() {
        handleSnackBar(new SnackbarData(SnackbarStatus.CLOSE));
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