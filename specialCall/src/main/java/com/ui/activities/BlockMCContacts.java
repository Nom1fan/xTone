package com.ui.activities;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.special.app.R;
import com.utils.SharedPrefUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by rony on 10/02/2016.
 */


public class BlockMCContacts extends Activity implements View.OnClickListener{

    private static final String TAG = BlockMCContacts.class.getSimpleName();
    private RadioButton all_valid;
    private RadioButton contacts_only;
    private RadioButton blacklist_specific;
    private abstract class ActivityRequestCodes {
        public static final int SELECT_BLACK_LIST_CONTACTS = 10;
    }
    List<String> names = new ArrayList<String>();
    List<String> phones = new ArrayList<String>();
    List<String> allNames = new ArrayList<String>();
    List<String> allPhones = new ArrayList<String>();
    MyAdapter ma ;
    ListView lv;
    HashMap<String,String> blockedContacts ;
    Set<String> blockedContactsSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blocked_user_list);
        Log.i(TAG, "onCreate");
        getAllContacts(this.getContentResolver());
        blockedContactsSet = SharedPrefUtils.getStringSet(getApplicationContext(),SharedPrefUtils.SETTINGS,SharedPrefUtils.BLOCK_LIST);

        all_valid = (RadioButton) findViewById(R.id.all_valid);
        all_valid.setOnClickListener(this);
        contacts_only = (RadioButton) findViewById(R.id.contacts_only);
        contacts_only.setOnClickListener(this);
        blacklist_specific = (RadioButton) findViewById(R.id.blacklist_specific);
        blacklist_specific.setOnClickListener(this);

        String oldConfig = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.RADIO_BUTTON_SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME);
        if (oldConfig.isEmpty())
        {
            SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.RADIO_BUTTON_SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME, "ALL");
            all_valid.setChecked(true);
        }
        else
        {
            switch (oldConfig) {

                case "ALL":
                    all_valid.setChecked(true);
                    break;

                case "CONTACTS":
                    contacts_only.setChecked(true);
                    break;

                case "black_list":
                    blacklist_specific.setChecked(true);
                    break;
            }
        }

        if (blockedContactsSet!=null){

            lv= (ListView) findViewById(R.id.blv);
            lv.removeAllViewsInLayout();
            ma = new MyAdapter();
            lv.setAdapter(ma); // clear listview

            getAllContactsFromSetString(blockedContactsSet);

            lv.setAdapter(ma);
            lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            lv.setItemsCanFocus(false);
            lv.setTextFilterEnabled(true);


            Log.i(TAG, "blockedSet Size : " + String.valueOf(blockedContactsSet.size()));
        }
        else
        {
            blockedContactsSet =  new HashSet<String>();
            Log.i(TAG,"blockedSet Initialize : " + String.valueOf(blockedContactsSet.size()));
        }

    }
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.all_valid) {

            SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.RADIO_BUTTON_SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME, "ALL");

        }
        if (id == R.id.contacts_only) {

            SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.RADIO_BUTTON_SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME, "CONTACTS");

        }
        if (id == R.id.blacklist_specific) {

            SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.RADIO_BUTTON_SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME, "black_list");

            Intent mainIntent = new Intent(BlockMCContacts.this,
                    SelectSpecificContacts.class);

            if (blockedContacts!= null)
            Log.i(TAG ,"SelectSpecificcontact blockedContacts  " + String.valueOf(blockedContacts.size()));
            if (blockedContactsSet!= null)
            Log.i(TAG, "SelectSpecificcontact blockedContactsSet  " + String.valueOf(blockedContactsSet.size()));
            mainIntent.putExtra("map", blockedContacts);

            startActivityForResult(mainIntent, ActivityRequestCodes.SELECT_BLACK_LIST_CONTACTS);

        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == ActivityRequestCodes.SELECT_BLACK_LIST_CONTACTS) {

                blockedContacts = (HashMap<String, String>)data.getSerializableExtra("result");
               // Toast.makeText(getApplicationContext(), " ACTIVITY RESULT FOR BLOCK LIST HashMapCount: " + String.valueOf(blockedContacts.size()), Toast.LENGTH_LONG).show();

                names = new ArrayList<String>();
                phones = new ArrayList<String>();

                lv= (ListView) findViewById(R.id.blv);
                lv.removeAllViewsInLayout();
                ma = new MyAdapter();
                lv.setAdapter(ma); // clear listview

                getAllContactsfromHashmap(blockedContacts); // populate the data for the adapter

                blockedContactsSet = SharedPrefUtils.getStringSet(getApplicationContext(),SharedPrefUtils.SETTINGS,SharedPrefUtils.BLOCK_LIST);
                if (blockedContacts!= null)
                Log.i(TAG ,"ActivityResult blockedContacts  " + String.valueOf(blockedContacts.size()));
                if (blockedContactsSet!= null)
                Log.i(TAG ,"ActivityResult blockedContactsSet  " + String.valueOf(blockedContactsSet.size()));
                        lv.setAdapter(ma);
                lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                lv.setItemsCanFocus(false);
                lv.setTextFilterEnabled(true);

            }
        }
    }

    public  void getAllContactsfromHashmap(HashMap selectedContacts) {
        Iterator myVeryOwnIterator = selectedContacts.keySet().iterator();
        while(myVeryOwnIterator.hasNext()) {
            String name = (String) myVeryOwnIterator.next();
            String phoneNumber = (String) selectedContacts.get(name);

            if (!phones.contains(phoneNumber)) {
                names.add(name);
                phones.add(phoneNumber);
            }
        }
      }

    public  void getAllContactsFromSetString(Set<String> storedContacts) {

        for (String phone : allPhones)
        {
            if (storedContacts.contains(phone))
            {
                names.add(allNames.get(allPhones.indexOf(phone)));
                phones.add(phone);
            }
        }
    }

    public  void getAllContacts(ContentResolver cr) {

        Cursor curPhones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (curPhones.moveToNext())
        {
            String name=curPhones.getString(curPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = toValidPhoneNumber(curPhones.getString(curPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));

            if (!phones.contains(phoneNumber) && (phoneNumber.length() == 10))
            {  allNames.add(name);
                allPhones.add(phoneNumber);
            }

        }
        curPhones.close();
    }

    private String toValidPhoneNumber(String str) {

        str = str.replaceAll("[^0-9]","");

        if (str.startsWith("972")){
            str= str.replaceFirst("972","0");
        }
        if (str.startsWith("9720")){
            str= str.replaceFirst("9720","0");
        }

        return str;
    }

    class MyAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener//,Filterable
    {  private SparseBooleanArray mCheckStates;
        LayoutInflater mInflater;
        TextView tv1,tv;
        Button cb;
        // filter_here filter = new filter_here();
        MyAdapter()
        {
            mCheckStates = new SparseBooleanArray(names.size());
            mInflater = (LayoutInflater)BlockMCContacts.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //   filter = new filter_here();

        }
        @Override
        public int getCount() {
            return names.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {

            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View vi=convertView;
            if(convertView==null)
                vi = mInflater.inflate(R.layout.blacklist_row, null);
            tv= (TextView) vi.findViewById(R.id.blacklist_name);
            tv1= (TextView) vi.findViewById(R.id.blacklist_phone);
            cb = (Button) vi.findViewById(R.id.remove_from_blacklist);
            tv.setText(names.get(position));
            tv1.setText(phones.get(position));
            cb.setTag(position);



            View.OnClickListener buttonListener = new View.OnClickListener() {

                @Override
                public void onClick(View v) {


                    int position = lv.getPositionForView(v);

                    Log.i(TAG, "position: " + String.valueOf(position));

                    if (blockedContacts !=null)
                    {  blockedContacts.remove((names.get(position).toString()));
                    Log.i(TAG, "Removed from BlockedContacts: " + (names.get(position).toString()));
                    }

                    lv.removeViewInLayout(v);

                    names.remove(position);


                    ////// REMOVING From Black List is SharedPreferences

                    Log.i(TAG, "Remove from Set: " + phones.get(position));
                  if (blockedContactsSet!=null)
                    blockedContactsSet.remove(phones.get(position));
                    phones.remove(position);

                    SharedPrefUtils.setStringSet(getApplicationContext(), SharedPrefUtils.SETTINGS, SharedPrefUtils.BLOCK_LIST, blockedContactsSet);
                    ((BaseAdapter)ma).notifyDataSetChanged();


                }
            };

            cb.setOnClickListener(buttonListener);





            return vi;
        }


        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        }
    }

}
