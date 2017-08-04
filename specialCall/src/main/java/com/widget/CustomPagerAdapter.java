package com.widget;

import android.content.Context;
import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.enums.ModelObject;
import com.mediacallz.app.R;
import com.ui.activities.ContactMCSelectionActivity;
import com.ui.activities.SelectMediaActivity;
import com.utils.CacheUtils;
import com.utils.SharedPrefUtils;

import java.util.HashMap;
import java.util.Map;

public class CustomPagerAdapter extends PagerAdapter {

    private final String TAG = CustomPagerAdapter.class.getSimpleName();
    private Map<Integer, View> pageMap =  new HashMap<Integer,View>();    //records all the pages in the ViewPager
    private SearchView searchView;
    private Context mContext;
    private View view=null;
    public CustomPagerAdapter(Context context) {
        mContext = context;
    }
    private OnlineContactAdapter onlineListOfContactsAdapter = null;
    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        ModelObject modelObject = ModelObject.values()[position];
        LayoutInflater inflater = (LayoutInflater) collection.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Log.i(TAG, "getLayoutResId:" +modelObject.getLayoutResId()  + " POSITION:" + position);
        switch (position) {
            case 0:

                Log.i(TAG, " case 0: getLayoutResId:" +modelObject.getLayoutResId()  );
                view = (ViewGroup) inflater.inflate(modelObject.getLayoutResId(), collection, false);

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
                           // InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                           // imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                            enableUserRegisterFunctionality(destPhoneNumber,destName);
                        } else {
                            enableInviteForUnregisteredUserFunctionality(destName);
                        }

                    }
                });

                break;
            case 1:



                Log.i(TAG, " case 1: getLayoutResId:" +modelObject.getLayoutResId()  );
                view = (ViewGroup) inflater.inflate(modelObject.getLayoutResId(), collection, false);
                ListView historyRecordsLV = (ListView)view.findViewById(R.id.calls_log_history_lv);

                if (CacheUtils.cachedCallHistoryList == null)
                    break;

                CallRecordsAdapter historyRecordsAdapter = new CallRecordsAdapter(view.getContext(), CacheUtils.cachedCallHistoryList,CacheUtils.cachedCallHistoryList);
                historyRecordsLV.setAdapter(historyRecordsAdapter);
                break;

            default:



                Log.i(TAG, " default: getLayoutResId:" +modelObject.getLayoutResId()  );
                view = (ViewGroup) inflater.inflate(modelObject.getLayoutResId(), collection, false);
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
                            enableInviteForUnregisteredUserFunctionality(destName);
                        }

                    }
                });

                break;

        }


        ((ViewPager) collection).addView(view, 0);
        if (view!=null)
            pageMap.put(position,view);
        return view;
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

    public View retreiveCurrentPageView(int position){
        return pageMap.get(position);
    }

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

    private void enableInviteForUnregisteredUserFunctionality(String name) {
       // InviteDialog inviteDialog = new InviteDialog(name);
       // inviteDialog.show(, TAG);
    }

}

