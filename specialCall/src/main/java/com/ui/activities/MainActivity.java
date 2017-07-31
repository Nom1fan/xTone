package com.ui.activities;

import android.animation.ObjectAnimator;
import android.app.LoaderManager;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
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
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.app.AppStateManager;
import com.async.tasks.IsRegisteredTask;
import com.async.tasks.SendBugEmailAsyncTask;
import com.batch.android.Batch;
import com.crashlytics.android.Crashlytics;
import com.data.objects.ActivityRequestCodes;
import com.data.objects.CallHistoryRecord;
import com.data.objects.Constants;
import com.data.objects.ContactWrapper;
import com.data.objects.KeysForBundle;
import com.data.objects.SnackbarData;
import com.enums.CallRecordType;
import com.enums.SpecialMediaType;
import com.enums.UserStatus;
import com.event.Event;
import com.event.EventReport;
import com.files.media.MediaFile;
import com.flows.UploadFileFlow;
import com.flows.WaitForTransferSuccessPostUploadFileFlowLogic;
import com.interfaces.ICallbackListener;
import com.mediacallz.app.R;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.services.IncomingService;
import com.services.OutgoingService;
import com.services.ServerProxyService;
import com.ui.dialogs.InviteDialog;
import com.ui.dialogs.MandatoryUpdateDialog;
import com.utils.ContactsUtils;
import com.utils.InitUtils;
import com.utils.MediaFileProcessingUtils;
import com.utils.PhoneNumberUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;
import com.utils.UtilityFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.crashlytics.android.Crashlytics.log;
import static com.crashlytics.android.Crashlytics.setUserIdentifier;
import static com.data.objects.SnackbarData.SnackbarStatus;
import static com.mediacallz.app.R.layout.activity_main;

public class MainActivity extends AppCompatActivity implements OnClickListener, ICallbackListener, LoaderManager.LoaderCallbacks<Cursor> {

    private final String TAG = MainActivity.class.getSimpleName();
    private String destPhoneNumber = "";
    private String destName = "";
    private static final int URL_LOADER = 1;
    private List<CallHistoryRecord> arrayOfRecords;
    private CallRecordsAdapter adapterForCallsRecords;
    private ListView callListView;

    //region UI elements
    private BroadcastReceiver eventReceiver;
    private IntentFilter eventIntentFilter = new IntentFilter(Event.EVENT_ACTION);
    private ListView drawerList;
    private ListView contactsListView;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private RelativeLayout mainActivityLayout;
    private boolean openDrawer = false;
    private Snackbar snackBar;
    private UploadFileFlow uploadFileFlow = new UploadFileFlow();
    private List<ContactWrapper> arrayOfUsers;
    private OnlineContactAdapter adapter;
    private SearchView searchView;
    private InitUtils initUtils = UtilityFactory.instance().getUtility(InitUtils.class);
    private final ContactsUtils contactsUtils = UtilityFactory.instance().getUtility(ContactsUtils.class);

    private ImageButton mainTab;
    private ImageButton callHistoryTab;
    private ObjectAnimator objectAnimator;


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

        if (AppStateManager.isLoggedIn(this)) { // should always start from idle and registeredContactLV
            AppStateManager.setAppState(getApplicationContext(), TAG, AppStateManager.STATE_IDLE);
            AppStateManager.setAppState(this, TAG, AppStateManager.getAppPrevState(this));

            getLoaderManager().initLoader(URL_LOADER, null, MainActivity.this);
            ServerProxyService.getRegisteredContacts(getApplicationContext());
            initializeUI();
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

        arrayOfRecords = new ArrayList<>();

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


        // Create the adapter to convert the array to views
        adapterForCallsRecords = new CallRecordsAdapter(this, arrayOfRecords);
        // Attach the adapter to a ListView
        callListView.setAdapter(adapter);
        adapterForCallsRecords.notifyDataSetChanged();

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset()");
        // do nothing
    }

    public class CallRecordsAdapter extends ArrayAdapter<CallHistoryRecord> implements Filterable {
        private List<CallHistoryRecord> allRecords;
        private List<CallHistoryRecord> dynamicRecords;

        CallRecordsAdapter(Context context, List<CallHistoryRecord> allRecords) {
            super(context, 0, allRecords);
            this.allRecords = allRecords;
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
                        List<CallHistoryRecord> found = new ArrayList<>();
                        dynamicRecords = new ArrayList<>(arrayOfRecords);
                        for (CallHistoryRecord record : dynamicRecords) {
                            if (record.getNameOrNumber().toLowerCase().contains(constraint)) {
                                found.add(record);
                            }
                        }
                        result.values = found;
                        result.count = found.size();
                    } else {
                        result.values = dynamicRecords;
                        if (dynamicRecords != null)
                            result.count = dynamicRecords.size();
                    }
                    return result;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clear();
                    if (results.values != null)
                        for (CallHistoryRecord record : (List<CallHistoryRecord>) results.values) {
                            add(record);
                        }
                    notifyDataSetChanged();
                }
            };
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // Get the data item for this position

            CallHistoryRecord callRecord = new CallHistoryRecord();
            callRecord = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.call_record_row, parent, false);
            }
            // Lookup view for data population
            TextView tvNameOrPhone = (TextView) convertView.findViewById(R.id.call_name_or_number);
            TextView tvDateAndTime = (TextView) convertView.findViewById(R.id.call_record_date_time);
            TextView tvDuration = (TextView) convertView.findViewById(R.id.call_record_duration);
            ImageView callRecordDirectionImage = (ImageView) convertView.findViewById(R.id.call_record_direction);
            // Populate the data into the template view using the data object
            tvNameOrPhone.setText(callRecord != null ? callRecord.getNameOrNumber() : null);
            tvDateAndTime.setText(callRecord != null ? callRecord.getDateAndTime() : null);
            tvDuration.setText(callRecord != null ? callRecord.getDuration() : null);

            if (callRecord.getCallType() != null) {
                if (callRecord != null && callRecord.getCallType().equals(CallRecordType.INCOMING)) {
                    callRecordDirectionImage.setImageResource(android.R.drawable.sym_call_incoming);
                    callRecordDirectionImage.setTag("incoming");
                } else if (callRecord != null && callRecord.getCallType().equals(CallRecordType.OUTGOING)) {
                    callRecordDirectionImage.setImageResource(android.R.drawable.sym_call_outgoing);
                    callRecordDirectionImage.setTag("outgoing");
                } else if (callRecord != null && callRecord.getCallType().equals(CallRecordType.MISSED)) {
                    callRecordDirectionImage.setImageResource(android.R.drawable.sym_call_missed);
                    callRecordDirectionImage.setTag("missed");
                } else {
                    callRecordDirectionImage.setImageResource(android.R.drawable.ic_menu_help);
                    callRecordDirectionImage.setTag("UKNOWN");
                }
            } else {
                callRecordDirectionImage.setImageResource(android.R.drawable.ic_menu_help);
                callRecordDirectionImage.setTag("UKNOWN");
            }
            // Return the completed view to render on screen
            return convertView;
        }
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
        Object userManager = getSystemService("user");
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


            prepareEventReceiver();

            if (!appState.equals(AppStateManager.STATE_LOADING))
                handleSnackBar(new SnackbarData(SnackbarStatus.CLOSE, 0, 0, null));

            restoreInstanceState();

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

            objectAnimator = ObjectAnimator
                    .ofInt(contactsListView, "scrollY", contactsListView.getBottom())
                    .setDuration(3000);
            objectAnimator.start();

            objectAnimator = ObjectAnimator
                    .ofInt(callListView, "scrollY", callListView.getBottom())
                    .setDuration(3000);
            objectAnimator.start();

            contactsListView.performClick();

            contactsListView.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
                @Override
                public void onSwipeLeft() {
                    SwitchToCallHistory();
                }

                @Override
                public void onSwipeRight() {
                    SwitchToCallHistory();
                }

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        objectAnimator.cancel();
                    }
                    return false;
                }
            });

            callListView.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
                @Override
                public void onSwipeLeft() {
                    SwitchToOnlineContacts();
                }

                @Override
                public void onSwipeRight() {
                    SwitchToOnlineContacts();

                }

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        objectAnimator.cancel();
                    }
                    return false;
                }

            });

        }
    }

    //region NONEED


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
                        List<ContactWrapper> found = new ArrayList<>();
                        dynamicContacts = new ArrayList<>(arrayOfUsers);
                        for (ContactWrapper contactWrapper : dynamicContacts) {
                            if (contactWrapper.getContact().getName().toLowerCase().contains(constraint) || contactWrapper.getContact().getPhoneNumber().contains(constraint)) {
                                found.add(contactWrapper);
                            }
                        }
                        result.values = found;
                        result.count = found.size();
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
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setOnQueryTextListener(onQueryTextListener());
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
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

    public void onClick(View v) {

        // Saving instance state
        saveInstanceState();

        int id = v.getId();
        if (id == R.id.call_history_main_btn) {
            SwitchToCallHistory();
        }else if (id == R.id.mediacallz_main_btn) {
            SwitchToOnlineContacts();
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
                PopulateOnlineContactsListView();

                break;

            default: // Event not meant for MainActivity receiver
        }
    }

    private void PopulateOnlineContactsListView() {
        adapter = new OnlineContactAdapter(this, arrayOfUsers);
        // Attach the adapter to a ListView
        contactsListView.setAdapter(adapter);
        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                destPhoneNumber = ((TextView) view.findViewById(R.id.contact_phone)).getText().toString();
                String status_tag = String.valueOf(view.findViewById(R.id.contact_status).getTag());
                destName = ((TextView) view.findViewById(R.id.contact_name)).getText().toString();

                SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION, true);

                if (status_tag.equals("on")) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                    enableUserRegisterFunctionality();
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
    }

    private void enableUserRegisterFunctionality() {
        Intent mainIntent = new Intent(this, ContactMCSelectionActivity.class);
        mainIntent.putExtra(SelectMediaActivity.DESTINATION_NUMBER, destPhoneNumber);
        mainIntent.putExtra(SelectMediaActivity.DESTINATION_NAME, destName);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
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


    }

    private void restoreInstanceState() {


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

        prepareMainActivityLayout();
        setCustomActionBar();
        enableHamburgerIconWithSlideMenu();
        prepareStartingView();
    }

    private void prepareStartingView() {

        contactsListView = (ListView) findViewById(R.id.online_contacts);
        mainTab  = (ImageButton) findViewById(R.id.mediacallz_main_btn);
        callHistoryTab  = (ImageButton) findViewById(R.id.call_history_main_btn);
        callListView = (ListView) findViewById(R.id.calls_log_history_lv);
        enableActivityBar();

    }

    private void SwitchToOnlineContacts(){
        Log.i(TAG, "StartSwitchToOnlineContacts()");

       // initializeUI();
        contactsListView.setVisibility(View.VISIBLE);
        callListView.setVisibility(View.INVISIBLE);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        initializeUI();
        Log.i(TAG, "SwitchToOnlineContacts(): " + arrayOfUsers.size());
    }

    private void SwitchToCallHistory(){
        Log.i(TAG, "StartSwitchToCallHistory()");
      //  initializeUI();
        callListView.setVisibility(View.VISIBLE);
        contactsListView.setVisibility(View.INVISIBLE);


        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        initializeUI();
        Log.i(TAG, "SwitchToCallHistory(): " + arrayOfRecords.size());
    }


    private void enableActivityBar() {
        mainTab.setVisibility(View.VISIBLE);

        mainTab.setClickable(true);

        mainTab.setOnClickListener(this);

        callHistoryTab.setVisibility(View.VISIBLE);

        callHistoryTab.setClickable(true);

        callHistoryTab.setOnClickListener(this);

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

    private void prepareMainActivityLayout() {

        mainActivityLayout = (RelativeLayout) findViewById(R.id.mainActivity);
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

        saveInstanceState();
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, DefaultMediaActivity.class);
        startActivity(intent);
    }

    private void BlockMCContacts() {
        saveInstanceState();
        Intent y = new Intent();
        y.setClass(this, BlockMCContacts.class);
        startActivity(y);
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
                          //  drawSelectProfileMediaButton(false);
                            break;

                        case IsRegisteredTask.ENABLE_FETCH_PROGRESS_BAR:
                          //  enableUserFetchProgressBar();
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

    /**
     * Detects left and right swipes across a view.
     */
    public class OnSwipeTouchListener implements View.OnTouchListener {

        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        public void onSwipeLeft() {

        }



        public void onSwipeRight() {

        }

        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_DISTANCE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float distanceX = e2.getX() - e1.getX();
                float distanceY = e2.getY() - e1.getY();
                if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (distanceX > 0)
                        onSwipeRight();
                    else
                        onSwipeLeft();
                    return true;
                }
                return false;
            }
        }
    }



}
