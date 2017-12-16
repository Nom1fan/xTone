package com_international.widget;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com_international.async.tasks.IsRegisteredTask;
import com_international.enums.ModelObject;
import com_international.interfaces.ICallbackListener;
import com_international.mediacallz.app.R;
import com_international.ui.activities.ContactMCSelectionActivity;
import com_international.ui.activities.SelectMediaActivity;
import com_international.ui.dialogs.InviteDialog;
import com_international.utils.CacheUtils;
import com_international.utils.PhoneNumberUtils;
import com_international.utils.SharedPrefUtils;

import static com_international.ui.activities.MainActivity.componentName;
import static com_international.ui.activities.MainActivity.fragmanager;
import static com_international.ui.activities.MainActivity.mainActivityMenu;

public class CustomPagerAdapter extends PagerAdapter implements ICallbackListener {

    private static final int REQUEST_PHONE_CALL = 666;
    private final String TAG = CustomPagerAdapter.class.getSimpleName();
    private Context mContext;
    private View view=null;
    public CustomPagerAdapter(Context context) {
        mContext = context;
    }
    private OnlineContactAdapter onlineListOfContactsAdapter = null;
    private SearchView searchView;
    private EditText EditTextPhoneNumber;

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        ModelObject modelObject = ModelObject.values()[position];
        LayoutInflater inflater = (LayoutInflater) collection.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Log.i(TAG, "getLayoutResId:" +modelObject.getLayoutResId()  + " POSITION:" + position);
        switch (position) {
         //region case 0
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
         //endregion
         //region case 1
            case 1:

                Log.i(TAG, " case 1: getLayoutResId:" +modelObject.getLayoutResId()  );
                view = inflater.inflate(modelObject.getLayoutResId(), collection, false);
                ListView historyRecordsLV = (ListView)view.findViewById(R.id.calls_log_history_lv);

                if (CacheUtils.cachedCallHistoryList == null)
                    break;

                CallRecordsAdapter historyRecordsAdapter = new CallRecordsAdapter(view.getContext(), CacheUtils.cachedCallHistoryList,CacheUtils.cachedCallHistoryList);
                historyRecordsLV.setAdapter(historyRecordsAdapter);
                break;
        //endregion
         //region case 2
            case 2:

                Log.i(TAG, " case 1: getLayoutResId:" +modelObject.getLayoutResId()  );
                view = inflater.inflate(modelObject.getLayoutResId(), collection, false);


                ListView dailerListView = (ListView)view.findViewById(R.id.dailer_listview);
                EditTextPhoneNumber = (EditText) view.findViewById(R.id.EditTextPhoneNumber);

                if (CacheUtils.cachedContactList == null)
                    break;

                retreiveOnlineAdapter();
                dailerListView.setAdapter(onlineListOfContactsAdapter);

                dailerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String destPhoneNumber = ((TextView) view.findViewById(R.id.contact_phone)).getText().toString();
                        EditTextPhoneNumber.setText(destPhoneNumber);

                    }
                });

                EditTextPhoneNumber.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                        // TODO Auto-generated method stub
                        onlineListOfContactsAdapter.getFilter().filter(EditTextPhoneNumber.getText().toString());
                    }

                    @Override
                    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                                  int arg3) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void afterTextChanged(Editable arg0) {
                        // TODO Auto-generated method stub

                    }
                });


                onlineListOfContactsAdapter.notifyDataSetChanged();

                ImageButton btn1 = (ImageButton) view.findViewById(R.id.Button1);
                ImageButton btn2 = (ImageButton) view.findViewById(R.id.Button2);
                ImageButton btn3 = (ImageButton) view.findViewById(R.id.Button3);
                ImageButton btn4 = (ImageButton) view.findViewById(R.id.Button4);
                ImageButton btn5 = (ImageButton) view.findViewById(R.id.Button5);
                ImageButton btn6 = (ImageButton) view.findViewById(R.id.Button6);
                ImageButton btn7 = (ImageButton) view.findViewById(R.id.Button7);
                ImageButton btn8 = (ImageButton) view.findViewById(R.id.Button8);
                ImageButton btn9 = (ImageButton) view.findViewById(R.id.Button9);
                ImageButton btn0 = (ImageButton) view.findViewById(R.id.Button0);
                ImageButton btnCall = (ImageButton) view.findViewById(R.id.ButtonCall);
                ImageButton btnDelete = (ImageButton) view.findViewById(R.id.ButtonDelete);
                ImageButton btnAsterrisk = (ImageButton) view.findViewById(R.id.ButtonStar);
                ImageButton btnHash = (ImageButton) view.findViewById(R.id.ButtonHash);


                btnHash.setOnClickListener(dialPadClickListener);
                btnAsterrisk.setOnClickListener(dialPadClickListener);
                btn1.setOnClickListener(dialPadClickListener);
                btn2.setOnClickListener(dialPadClickListener);
                btn3.setOnClickListener(dialPadClickListener);
                btn4.setOnClickListener(dialPadClickListener);
                btn5.setOnClickListener(dialPadClickListener);
                btn6.setOnClickListener(dialPadClickListener);
                btn7.setOnClickListener(dialPadClickListener);
                btn8.setOnClickListener(dialPadClickListener);
                btn9.setOnClickListener(dialPadClickListener);
                btn0.setOnClickListener(dialPadClickListener);
                btn0.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        EditTextPhoneNumber.setText(EditTextPhoneNumber.getText() + "+");
                        return true;
                    }
                });

                btnCall.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (ContextCompat.checkSelfPermission(view.getContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions((Activity) view.getContext(), new String[]{Manifest.permission.CALL_PHONE},REQUEST_PHONE_CALL);
                        }
                        else
                        {
                            Intent in = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+EditTextPhoneNumber.getText()));
                            try {
                                view.getContext().startActivity(in);
                            } catch (android.content.ActivityNotFoundException ex) {
                                Toast.makeText(view.getContext(), "Could not find an activity to place the call.", Toast.LENGTH_SHORT).show();
                            }
                        }

                    }
                });

                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                       if (!EditTextPhoneNumber.getText().toString().isEmpty())
                            EditTextPhoneNumber.setText(EditTextPhoneNumber.getText().toString().substring(0,EditTextPhoneNumber.length()-1));
                    }
                });

                btnDelete.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        EditTextPhoneNumber.setText("");
                        return true;
                    }
                });


                break;
            //endregion
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

    private View.OnClickListener dialPadClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            EditTextPhoneNumber.setText(EditTextPhoneNumber.getText() + arg0.getTag().toString());
        }
    };


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

