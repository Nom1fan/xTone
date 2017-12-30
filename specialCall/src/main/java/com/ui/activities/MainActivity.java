package com.ui.activities;

import android.app.LoaderManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.app.AppStateManager;
import com.async.tasks.SendBugEmailAsyncTask;
import com.crashlytics.android.Crashlytics;
import com.data.objects.ActivityRequestCodes;
import com.data.objects.CallHistoryRecord;
import com.data.objects.Constants;
import com.data.objects.ContactWrapper;
import com.data.objects.KeysForBundle;
import com.data.objects.SnackbarData;
import com.enums.CallRecordType;
import com.enums.SpecialMediaType;
import com.event.Event;
import com.event.EventReport;
import com.files.media.MediaFile;
import com.flows.UploadFileFlow;
import com.flows.WaitForTransferSuccessPostUploadFileFlowLogic;
import com.mediacallz.app.R;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.services.IncomingService;
import com.services.OutgoingService;
import com.services.ServerProxyService;
import com.ui.dialogs.InviteDialog;
import com.ui.dialogs.MandatoryUpdateDialog;
import com.utils.CacheUtils;
import com.utils.InitUtils;
import com.utils.MediaFileProcessingUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;
import com.utils.UtilityFactory;
import com.widget.CustomPagerAdapter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.crashlytics.android.Crashlytics.log;
import static com.crashlytics.android.Crashlytics.setUserIdentifier;
import static com.data.objects.SnackbarData.SnackbarStatus;
import static com.mediacallz.app.R.layout.activity_main;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private final String TAG = MainActivity.class.getSimpleName();
    private String destPhoneNumber = "";
    private String destName = "";
    private static final int URL_LOADER = 1;
    private CustomPagerAdapter customePageAdapter;

    //region UI elements
    private BroadcastReceiver eventReceiver;
    private IntentFilter eventIntentFilter = new IntentFilter(Event.EVENT_ACTION);
    private ListView drawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private boolean openDrawer = false;
    private Snackbar snackBar;
    private UploadFileFlow uploadFileFlow = new UploadFileFlow();
    private SearchView searchView;
    public static Menu mainActivityMenu;
    public static ComponentName componentName;
    public static android.app.FragmentManager fragmanager;

    private InitUtils initUtils = UtilityFactory.instance().getUtility(InitUtils.class);


    //endregion

    //region Activity methods (onCreate(), onPause(), onActivityResult()...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        //region onCreateBasic
        startLoginActivityIfLoggedOut();

        // so we can know who device was crashed, and get it's phone number.
        Crashlytics.setUserIdentifier(Constants.MY_ID(getApplicationContext()));

        if (AppStateManager.didAppCrash(this)) {
            Log.w(TAG, "Detected app previously crashed. Handling...");
            AppStateManager.setAppState(this, TAG, AppStateManager.getAppPrevState(this));
            AppStateManager.setDidAppCrash(this, false);
        }
//endregion

        if (AppStateManager.isLoggedIn(this)) { // should always start from idle and registeredContactLV
            AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_IDLE);
            AppStateManager.setAppState(this, TAG, AppStateManager.getAppPrevState(this));

            getLoaderManager().initLoader(URL_LOADER, null, MainActivity.this);
            ServerProxyService.getRegisteredContacts(getApplicationContext());

            initializeUI();

            ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
            customePageAdapter = new CustomPagerAdapter(this);
            viewPager.setAdapter(customePageAdapter);

           /* final ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrollStateChanged(int arg0) {
                    // TODO Auto-generated method stub
                    Log.i(TAG, "onPageScrollStateChanged");

                }

                @Override
                public void onPageScrolled(int arg0, float arg1, int arg2) {
                    Log.i(TAG, "onPageScrolled");

                }

                @Override
                public void onPageSelected(int pos) {
                    Log.i(TAG, "onPageSelected");
                    switch (pos) {
                        case 0:
                            searchView.setVisibility(View.VISIBLE);
                            break;
                        case 1:
                            searchView.setVisibility(View.GONE);
                             InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                             imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                            break;
                    }
                }
            };
            viewPager.addOnPageChangeListener(mPageChangeListener);*/
            fragmanager = getFragmentManager();
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle args) {
        Log.d(TAG, "onCreateLoader() >> loaderID : " + loaderID);

        switch (loaderID) {
            case URL_LOADER:
                // Returns a new CursorLoader
                return new CursorLoader(
                        this,   // Parent activity context
                        CallLog.Calls.CONTENT_URI,        // Table to query
                        null,     // Projection to return
                        null,            // No selection clause
                        null,            // No selection arguments
                        null             // Default sort order
                );
            default:
                return null;
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor managedCursor) {
        Log.d(TAG, "onLoadFinished()");

        List<CallHistoryRecord> arrayOfRecords = new ArrayList<>();

        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);

        while (managedCursor.moveToNext()) {
            String phNumber = managedCursor.getString(number);
            String callType = managedCursor.getString(type);
            String callDate = managedCursor.getString(date);
            Date callDayTime = new Date(Long.valueOf(callDate));
            String callDuration = managedCursor.getString(duration);
            CallRecordType dir = null;

            int callTypeCode = Integer.parseInt(callType);
            switch (callTypeCode) {
                case CallLog.Calls.OUTGOING_TYPE:
                    dir = CallRecordType.OUTGOING;
                    break;

                case CallLog.Calls.INCOMING_TYPE:
                    dir = CallRecordType.INCOMING;
                    break;

                case CallLog.Calls.MISSED_TYPE:
                    dir = CallRecordType.MISSED;
                    break;
            }

            CallHistoryRecord record = new CallHistoryRecord(phNumber, dir, callDayTime.toString(), callDuration);


            arrayOfRecords.add(record);

        }

        managedCursor.close();

        CacheUtils.cachedCallHistoryList = arrayOfRecords;
        customePageAdapter.notifyDataSetChanged();

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset()");
        // do nothing
    }

    //region tryout
    private void ifHuaweiAlert() {
        final SharedPreferences settings = getSharedPreferences("ProtectedApps", MODE_PRIVATE);
        final String saveIfSkip = "skipProtectedAppsMessage";
        boolean skipMessage = settings.getBoolean(saveIfSkip, false);
        if (!skipMessage) {
            final SharedPreferences.Editor editor = settings.edit();
            Intent intent = new Intent();
            intent.setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity");
            if (isCallable(intent)) {
                final AppCompatCheckBox dontShowAgain = new AppCompatCheckBox(this);
                dontShowAgain.setText("Do not show again");
                dontShowAgain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        editor.putBoolean(saveIfSkip, isChecked);
                        editor.apply();
                    }
                });

                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Huawei Protected Apps")
                        .setMessage(String.format("%s requires to be enabled in 'Protected Apps' to function properly.%n", getString(R.string.app_name)))
                        .setView(dontShowAgain)
                        .setPositiveButton("Protected Apps", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                huaweiProtectedApps();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                editor.putBoolean(saveIfSkip, true);
                editor.apply();
            }
        }
    }

    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private void huaweiProtectedApps() {
        try {
            String cmd = "am start -n com.huawei.systemmanager/.optimize.process.ProtectActivity";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                cmd += " --user " + getUserSerial();
            }
            Runtime.getRuntime().exec(cmd);
        } catch (IOException ignored) {
        }
    }

    private String getUserSerial() {
        //noinspection ResourceType
        Object userManager = getSystemService(Context.USER_SERVICE);
        if (null == userManager) return "";

        try {
            Method myUserHandleMethod = android.os.Process.class.getMethod("myUserHandle", (Class<?>[]) null);
            Object myUserHandle = myUserHandleMethod.invoke(android.os.Process.class, (Object[]) null);
            Method getSerialNumberForUser = userManager.getClass().getMethod("getSerialNumberForUser", myUserHandle.getClass());
            Long userSerial = (Long) getSerialNumberForUser.invoke(userManager, myUserHandle);
            if (userSerial != null) {
                return String.valueOf(userSerial);
            } else {
                return "";
            }
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException ignored) {
        }
        return "";
    }
    //endregion

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


            prepareEventReceiver();

            if (!appState.equals(AppStateManager.STATE_LOADING))
                handleSnackBar(new SnackbarData(SnackbarStatus.CLOSE, 0, 0, null));

            getAppRecord();

            syncAndroidVersionWithServer();

            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            boolean DNDGranted = notificationManager.isNotificationPolicyAccessGranted();
            Log.w(TAG, " DND mode GRANTED :" + DNDGranted);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && !DNDGranted) {

                Intent intent = new Intent(
                        android.provider.Settings
                                .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

                Log.w(TAG, "need to allow DND mode by user action");
                getApplicationContext().startActivity(intent);
            }


            // ifHuaweiAlert();
            initUtils.initSyncDefaultMediaReceiver(this);

        }
    }

    //region NONEED

    @Override // add search functionality
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // Inflate menu to add items to action bar if it is present.
        inflater.inflate(R.menu.select_contact_menu, menu);

        mainActivityMenu = menu;
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        componentName = getComponentName();

        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        log(Log.INFO, TAG, "onPause()");

        AppStateManager.setAppInForeground(this, false);

        if (eventReceiver != null) {
            try {
                unregisterReceiver(eventReceiver);
            } catch (Exception ex) {
                log(Log.ERROR, TAG, ex.getMessage());
            }
        }
        UI_Utils.dismissAllStandOutWindows(getApplicationContext());

            /* Apply our splash exit (fade out) and main
            entry (fade in) animation transitions. */
        if (openDrawer)
            overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation); // open drawer animation

    }

    @Override
    protected void onStop() {
        log(Log.INFO, TAG, "onStop()");

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        log(Log.INFO, TAG, "onDestroy()");

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
                        bundle.putString(KeysForBundle.DEST_ID, destPhoneNumber);
                        bundle.putString(KeysForBundle.DEST_NAME, destName);
                        bundle.putSerializable(KeysForBundle.SPEC_MEDIA_TYPE, specialMediaType);

                        uploadFileFlow.executeUploadFileFlow(MainActivity.this, bundle, new WaitForTransferSuccessPostUploadFileFlowLogic());

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
    //endregion

    //region Assisting methods (onClick(), eventReceived(), ...)

    private void startLoginActivityIfLoggedOut() {

        if (!AppStateManager.isLoggedIn(this)) {

            stateLoggedOut();
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
                enableInviteForUnregisteredUserFunctionality("",searchView.getQuery().toString());
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
                List<ContactWrapper> arrayOfUsers = new ArrayList<>((List<ContactWrapper>) event.report().data());
                CacheUtils.cachedContactList = arrayOfUsers;
                customePageAdapter.notifyDataSetChanged();
                break;

            default: // Event not meant for MainActivity receiver
        }
    }


    private void enableUserRegisterFunctionality() {
        Intent mainIntent = new Intent(this, ContactMCSelectionActivity.class);
        mainIntent.putExtra(SelectMediaActivity.DESTINATION_NUMBER, searchView.getQuery());
        mainIntent.putExtra(SelectMediaActivity.DESTINATION_NAME, destName);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
    }

    private void enableInviteForUnregisteredUserFunctionality(String name,String number) {
        InviteDialog inviteDialog = new InviteDialog(name,number);
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

    //endregion

    //region UI methods

    private void initializeUI() {

        setContentView(activity_main);

        setCustomActionBar();
        enableHamburgerIconWithSlideMenu();
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
    }

    //endregion

    //region MenuOptions

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
                    //   invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                }

                /**
                 * Called when a drawer has settled in a completely closed state.
                 */
                public void onDrawerClosed(View view) {
                    super.onDrawerClosed(view);
                    //  invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
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
        dataList.add(new DrawerItem(getResources().getString(R.string.default_media), R.drawable.color_mc_cyan));
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
            case 1: //Default media
                defaultMediaActivity();
                break;
            case 2: // Who Can MC me
                BlockMCContacts();
                break;
            case 3: // App Settings
                appSettings();
                break;
            case 4: // About & Help
                appAboutAndHelp();
                break;
            case 5: // Send Bug
                SendBugEmailAsyncTask sendBugEmailAsyncTask = new SendBugEmailAsyncTask(this);
                sendBugEmailAsyncTask.execute();
                break;
        }

        mDrawerLayout.closeDrawer(drawerList);
    }

    private void defaultMediaActivity() {

        Intent intent = new Intent();
        intent.setClass(MainActivity.this, DefaultMediaActivity.class);
        startActivity(intent);
    }

    private void BlockMCContacts() {
        Intent y = new Intent();
        y.setClass(this, BlockMCContacts.class);
        startActivity(y);
    }

    private void appSettings() {

        Intent intent = new Intent();
        intent.setClass(MainActivity.this, SetSettingsActivity.class);
        startActivity(intent);
    }

    private void appAboutAndHelp() {

        Intent intent = new Intent();
        intent.setClass(MainActivity.this, SetAboutHelpActivity.class);
        startActivity(intent);
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
