package com.ui.activities;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.app.AppStateManager;
import com.async.tasks.IsRegisteredTask;
import com.async.tasks.SendBugEmailAsyncTask;
import com.data.objects.CallHistoryRecord;
import com.data.objects.SnackbarData;
import com.enums.CallRecordType;
import com.event.Event;
import com.interfaces.ICallbackListener;
import com.mediacallz.app.R;
import com.ui.dialogs.MandatoryUpdateDialog;
import com.utils.PhoneNumberUtils;
import com.utils.UI_Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.crashlytics.android.Crashlytics.log;
import static com.data.objects.SnackbarData.SnackbarStatus;

public class CallsLogHistory extends AppCompatActivity implements OnClickListener, ICallbackListener,LoaderManager.LoaderCallbacks<Cursor> {

    private final String TAG = CallsLogHistory.class.getSimpleName();

    //region UI elements
    private ProgressBar fetchUserPbar;
    private BroadcastReceiver eventReceiver;
    private IntentFilter eventIntentFilter = new IntentFilter(Event.EVENT_ACTION);
    private ListView drawerList;
    private ListView callListView;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private RelativeLayout mainActivityLayout;
    private boolean openDrawer = false;
    private Snackbar snackBar;
    private List<CallHistoryRecord> arrayOfRecords;
    private CallRecordsAdapter adapter;
    private SearchView searchView;
    private MenuItem backBtn;
    private static final int URL_LOADER = 1;
    //endregion

    //region Activity methods (onCreate(), onPause(), onActivityResult()...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        getLoaderManager().initLoader(URL_LOADER, null, CallsLogHistory.this);
        initializeUI();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerLayout != null)
            mDrawerToggle.syncState();
    }


    @Override
    protected void onResume() {
        super.onResume();
        log(Log.INFO, TAG, "onResume()");
        openDrawer = false;

            callListView.setOnTouchListener(new OnSwipeTouchListener(CallsLogHistory.this) {
                @Override
                public void onSwipeLeft() {
                    Intent toCallHistory = new Intent(getApplicationContext(), MainActivity.class);
                    toCallHistory.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(toCallHistory);
                }

                @Override
                public void onSwipeRight() {
                    Intent toCallHistory = new Intent(getApplicationContext(), MainActivity.class);
                    toCallHistory.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(toCallHistory);
                }

            });





        
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

            CallHistoryRecord record = new CallHistoryRecord(phNumber,dir,callDayTime.toString(),callDuration);


            arrayOfRecords.add(record);

        }

        managedCursor.close();


        // Create the adapter to convert the array to views
        adapter = new CallRecordsAdapter(this, arrayOfRecords);
        // Attach the adapter to a ListView
        callListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset()");
        // do nothing
    }
    @Override
    public void onBackPressed() {
        Log.d("CDA", "onBackPressed Called");

        if (AppStateManager.getAppState(this) != AppStateManager.STATE_IDLE) {
            AppStateManager.setAppState(this, TAG, AppStateManager.STATE_IDLE);
            UI_Utils.refreshUI(this, new SnackbarData(SnackbarStatus.CLOSE));
            searchView.setVisibility(View.VISIBLE);
        } else
            this.finish();

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

            if (callRecord.getCallType() != null){
                if (callRecord != null && callRecord.getCallType().equals(CallRecordType.INCOMING)) {
                    callRecordDirectionImage.setImageResource(android.R.drawable.sym_call_incoming);
                    callRecordDirectionImage.setTag("incoming");
                } else if (callRecord != null && callRecord.getCallType().equals(CallRecordType.OUTGOING)) {
                    callRecordDirectionImage.setImageResource(android.R.drawable.sym_call_outgoing);
                    callRecordDirectionImage.setTag("outgoing");
                } else if (callRecord != null && callRecord.getCallType().equals(CallRecordType.MISSED)){
                    callRecordDirectionImage.setImageResource(android.R.drawable.sym_call_missed);
                    callRecordDirectionImage.setTag("missed");
                }else {
                    callRecordDirectionImage.setImageResource(android.R.drawable.ic_menu_help);
                    callRecordDirectionImage.setTag("UKNOWN");
                }
            }else {
                callRecordDirectionImage.setImageResource(android.R.drawable.ic_menu_help);
                callRecordDirectionImage.setTag("UKNOWN");
            }
            // Return the completed view to render on screen
            return convertView;
        }
    }

    @Override // add search functionality
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // Inflate menu to add items to action bar if it is present.
      /*  inflater.inflate(R.menu.select_contact_menu, menu);

        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setOnQueryTextListener(onQueryTextListener());
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));*/
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

        UI_Utils.dismissAllStandOutWindows(getApplicationContext());

            /* Apply our splash exit (fade out) and main
            entry (fade in) animation transitions. */
        if (openDrawer)
            overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation); // open drawer animation

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
        if (Integer.parseInt(Build.VERSION.SDK) > 5
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


    public void onClick(View v) {


        int id = v.getId();

    }

    private void BlockMCContacts() {
        Intent y = new Intent();
        y.setClass(this, BlockMCContacts.class);
        startActivity(y);
    }
    //endregion

    //region UI methods

    private void initializeUI() {

        setContentView(R.layout.calls_log_history);

        prepareMainActivityLayout();
        prepareStartingView();
        setCustomActionBar();
        enableHamburgerIconWithSlideMenu();
        prepareFetchUserProgressBar();
    }

    private void prepareStartingView() {

        callListView = (ListView) findViewById(R.id.calls_log_history_lv);
        callListView.setVisibility(View.VISIBLE);

    }


    //region UI States

    //region UI elements controls

    private void prepareMainActivityLayout() {

        mainActivityLayout = (RelativeLayout) findViewById(R.id.callsLogHistoryActivity);
    }

    private void prepareFetchUserProgressBar() {

        fetchUserPbar = (ProgressBar) findViewById(R.id.clh_fetchuserprogress);
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

        mDrawerLayout = (DrawerLayout) findViewById(R.id.clh_drawer_layout);
        if (mDrawerLayout != null) {
            drawerList = (ListView) findViewById(R.id.clh_left_drawer);
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
        intent.setClass(CallsLogHistory.this, DefaultMediaActivity.class);
        startActivity(intent);
    }

    private void appSettings() {

        Intent intent = new Intent();
        intent.setClass(CallsLogHistory.this, SetSettingsActivity.class);
        startActivity(intent);
    }

    private void appAboutAndHelp() {

        Intent intent = new Intent();
        intent.setClass(CallsLogHistory.this, SetAboutHelpActivity.class);
        startActivity(intent);
    }

    private void disableUserFetchProgressBar() {

        fetchUserPbar.setVisibility(ProgressBar.GONE);
    }

    private void enableUserFetchProgressBar() {

        fetchUserPbar.setVisibility(ProgressBar.VISIBLE);

    }

    private void writeInfoSnackBar(final SnackbarData snackBarData) {

        log(Log.INFO, TAG, "Snackbar showing:" + snackBarData.getText());

        int duration = snackBarData.getDuration();
        if (duration == Snackbar.LENGTH_LONG)
            duration = 3500;

        View mainActivity = findViewById(R.id.callsLogHistoryActivity);

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

    /**
     * Detects left and right swipes across a view.
     */
    public class OnSwipeTouchListener implements View.OnTouchListener {

        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        public void onSwipeLeft() {
            Intent toMainActivity = new Intent(getApplicationContext(), MainActivity.class);
            toMainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(toMainActivity);
        }



        public void onSwipeRight() {
            Intent toMainActivity = new Intent(getApplicationContext(), MainActivity.class);
            toMainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(toMainActivity);
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
