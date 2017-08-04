package com.widget;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.async.tasks.IsRegisteredTask;
import com.enums.ModelObject;
import com.interfaces.ICallbackListener;
import com.mediacallz.app.R;
import com.ui.activities.ContactMCSelectionActivity;
import com.ui.activities.SelectMediaActivity;
import com.ui.dialogs.InviteDialog;
import com.utils.CacheUtils;
import com.utils.PhoneNumberUtils;
import com.utils.SharedPrefUtils;

import static com.ui.activities.MainActivity.componentName;
import static com.ui.activities.MainActivity.fragmanager;
import static com.ui.activities.MainActivity.mainActivityMenu;

public class CustomPagerAdapter extends PagerAdapter implements ICallbackListener {

    private final String TAG = CustomPagerAdapter.class.getSimpleName();
    private Context mContext;
    private View view=null;
    public CustomPagerAdapter(Context context) {
        mContext = context;
    }
    private OnlineContactAdapter onlineListOfContactsAdapter = null;
    private SearchView searchView;

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        ModelObject modelObject = ModelObject.values()[position];
        LayoutInflater inflater = (LayoutInflater) collection.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Log.i(TAG, "getLayoutResId:" +modelObject.getLayoutResId()  + " POSITION:" + position);
        switch (position) {
            case 0:

                if (mainActivityMenu!=null)
                    searchView = (SearchView) mainActivityMenu.findItem(R.id.action_search).getActionView();

                Log.i(TAG, " case 0: getLayoutResId:" +modelObject.getLayoutResId()  );
                view = inflater.inflate(modelObject.getLayoutResId(), collection, false);

                ListView onlineListOfContactsLV = (ListView)view.findViewById(R.id.online_contacts);

                if (CacheUtils.cachedContactList == null)
                    break;

                retreiveOnlineAdapter();
                onlineListOfContactsLV.setAdapter(onlineListOfContactsAdapter);

                onlineListOfContactsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String destPhoneNumber = ((TextView) view.findViewById(R.id.contact_phone)).getText().toString();
                        String status_tag = String.valueOf(view.findViewById(R.id.contact_status).getTag());
                        String destName = ((TextView) view.findViewById(R.id.contact_name)).getText().toString();

                        SharedPrefUtils.setBoolean(view.getContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION, true);

                        if (status_tag.equals("on")) {
                            enableUserRegisterFunctionality(destPhoneNumber,destName);
                        } else {
                            enableInviteForUnregisteredUserFunctionality(destName,destPhoneNumber);
                        }

                    }
                });


                // Associate searchable configuration with the SearchView

                SearchManager searchManager = (SearchManager) view.getContext().getSystemService(Context.SEARCH_SERVICE);
                if (searchView!=null) {
                    searchView.setOnQueryTextListener(onQueryTextListener());
                    searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName));
                }
                onlineListOfContactsAdapter.notifyDataSetChanged();



                break;
            case 1:

                Log.i(TAG, " case 1: getLayoutResId:" +modelObject.getLayoutResId()  );
                view = inflater.inflate(modelObject.getLayoutResId(), collection, false);
                ListView historyRecordsLV = (ListView)view.findViewById(R.id.calls_log_history_lv);

                if (CacheUtils.cachedCallHistoryList == null)
                    break;

                CallRecordsAdapter historyRecordsAdapter = new CallRecordsAdapter(view.getContext(), CacheUtils.cachedCallHistoryList,CacheUtils.cachedCallHistoryList);
                historyRecordsLV.setAdapter(historyRecordsAdapter);
                break;

            default:



                Log.i(TAG, " default: getLayoutResId:" +modelObject.getLayoutResId()  );
                view = inflater.inflate(modelObject.getLayoutResId(), collection, false);
                ListView onlineListOfContactsLV1 = (ListView)view.findViewById(R.id.online_contacts);

                if (CacheUtils.cachedContactList == null)
                    break;

                OnlineContactAdapter onlineListOfContactsAdapter1 = new OnlineContactAdapter(view.getContext(), CacheUtils.cachedContactList,CacheUtils.cachedContactList);
                onlineListOfContactsLV1.setAdapter(onlineListOfContactsAdapter1);

                onlineListOfContactsLV1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String destPhoneNumber = ((TextView) view.findViewById(R.id.contact_phone)).getText().toString();
                        String status_tag = String.valueOf(view.findViewById(R.id.contact_status).getTag());
                        String destName = ((TextView) view.findViewById(R.id.contact_name)).getText().toString();

                        SharedPrefUtils.setBoolean(view.getContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.ENABLE_UI_ELEMENTS_ANIMATION, true);

                        if (status_tag.equals("on")) {
                            enableUserRegisterFunctionality(destPhoneNumber,destName);
                        } else {
                            enableInviteForUnregisteredUserFunctionality(destName,destPhoneNumber);
                        }

                    }
                });

                break;

        }


        collection.addView(view, 0);
        return view;
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

                    new IsRegisteredTask(ifOnlyPhoneNumber, CustomPagerAdapter.this).execute(view.getContext());

                    return false;
                }
                onlineListOfContactsAdapter.getFilter().filter(searchView.getQuery());
                return false;
            }
        };
    }


    //region MethosPagerAdapter
    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return ModelObject.values().length;
    }

    @Override
    public int getItemPosition(Object object){
        return POSITION_NONE;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        ModelObject customPagerEnum = ModelObject.values()[position];
        return mContext.getString(customPagerEnum.getTitleResId());
    }
//endregion

    public OnlineContactAdapter retreiveOnlineAdapter(){

        if (onlineListOfContactsAdapter == null)
            onlineListOfContactsAdapter = new OnlineContactAdapter(view.getContext(), CacheUtils.cachedContactList,CacheUtils.cachedContactList);;

        return onlineListOfContactsAdapter;
    }

    private void enableUserRegisterFunctionality(String number , String name) {
        Intent mainIntent = new Intent(view.getContext(), ContactMCSelectionActivity.class);
        mainIntent.putExtra(SelectMediaActivity.DESTINATION_NUMBER, number);
        mainIntent.putExtra(SelectMediaActivity.DESTINATION_NAME, name);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        view.getContext().startActivity(mainIntent);
    }

    private void enableInviteForUnregisteredUserFunctionality(String name,String number) {
        InviteDialog inviteDialog = new InviteDialog(name,number);
        inviteDialog.show(fragmanager,TAG);
    }

    //region ICallbackListener methods
    @Override
    public void doCallBackAction() {

    }

    @Override
    public void doCallBackAction(final Object... params) {



    }

    //endregion

}

