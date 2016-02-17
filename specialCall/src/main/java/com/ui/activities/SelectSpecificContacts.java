package com.ui.activities;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import com.special.app.R;
import com.utils.SharedPrefUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SelectSpecificContacts extends AppCompatActivity implements OnItemClickListener{

    private static final String TAG = SelectSpecificContacts.class.getSimpleName();
    List<String> names = new ArrayList<String>(); // the list that the adapter uses to populate the view
    List<String> phones = new ArrayList<String>(); // the list that the adapter uses to populate the view
    MyAdapter ma ;
    ListView lv;
    HashMap<String,String> blockedContacts ;
    Set<String> blockedSet = new HashSet<String>();
    HashMap<String,String> allContactsClicked ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_spec_contacts);
        Log.i(TAG,"onCreate");

        Intent intent = getIntent();
        blockedContacts = (HashMap<String, String>)intent.getSerializableExtra("map");  // get the

        Set<String> storedBlockedContacts = SharedPrefUtils.getStringSet(getApplicationContext(),SharedPrefUtils.SETTINGS,SharedPrefUtils.BLOCK_LIST);

        if (storedBlockedContacts!=null)
            blockedSet = storedBlockedContacts;
        else
            blockedSet =  new HashSet<String>();

       /* if (blockedContacts==null || blockedContacts.size()<1)
        {
            allContactsClicked = new HashMap<String,String>();
            blockedContacts = new HashMap<String,String>();
        }*/

        getAllContacts(this.getContentResolver()); // populate all contacts to view with checkboxes
        lv= (ListView) findViewById(R.id.lv);
        ma = new MyAdapter();
        lv.setAdapter(ma);  // link the listview with the adapter
        lv.setOnItemClickListener(this);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lv.setItemsCanFocus(false);
        lv.setTextFilterEnabled(true);

        Button selectall = (Button) findViewById(R.id.selectall);
        selectall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < phones.size(); i++) {
                   ma.setChecked(i, true);
                    lv.setItemChecked(i, true);
                    ma.notifyDataSetChanged();
                }
                ma.notifyDataSetChanged();

                blockedContacts = new HashMap<String, String>();
                blockedContacts.putAll(allContactsClicked);
                saveBlockedContacts();
            }
        });

        Button unselectall = (Button) findViewById(R.id.unselect);
        unselectall.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {

                for ( int i=0; i < phones.size(); i++) {
                    ma.setChecked(i, false);
                    lv.setItemChecked(i, false);
                    ma.notifyDataSetChanged();
                }
                lv= (ListView) findViewById(R.id.lv);

                getAllContacts(getApplicationContext().getContentResolver());
                lv= (ListView) findViewById(R.id.lv);
                ma = new MyAdapter();
                lv.setAdapter(ma);
                lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                lv.setItemsCanFocus(false);
                lv.setTextFilterEnabled(true);

                ma.notifyDataSetChanged();

                blockedSet = new HashSet<String>();
                blockedContacts = new HashMap<String, String>();

            }
        });

        Button back = (Button) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Back Button Pressed");
                returnWithResultIntent();
                finish();
            }
        });

    }

    // Search to get to the location of the contact
    private SearchView.OnQueryTextListener onQueryTextListener() {
        return new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                int position = 0;
                while (position < names.size() - 1) {
                    if (names.get(position).toUpperCase().contains(s.toUpperCase())) {
                        lv.smoothScrollToPositionFromTop(position, 0, 200);
                        break;
                    } else {
                        position++;
                    }
                }

                return false;
            }
        };
    }

    @Override // add search functionality
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // Inflate menu to add items to action bar if it is present.
        inflater.inflate(R.menu.select_contact_menu, menu);
        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setOnQueryTextListener(onQueryTextListener());
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        returnWithResultIntent();
        saveBlockedContacts();
        super.onPause();
    }

    private void returnWithResultIntent() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", blockedContacts);

        if (getParent() == null) {
            setResult(Activity.RESULT_OK, returnIntent);
        } else {
            getParent().setResult(Activity.RESULT_OK, returnIntent);
        }
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG,"onBackPressed");
        returnWithResultIntent();
        super.onBackPressed();
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

    // saving black listed contacts to SharedPref
    private void saveBlockedContacts() {
        Iterator myVeryOwnIterator = blockedContacts.keySet().iterator();
        blockedSet = new HashSet<String>();
        while(myVeryOwnIterator.hasNext()) {
            String name = (String) myVeryOwnIterator.next();
            String phoneNumber = (String) blockedContacts.get(name);
            ////// ADDING  To Black List is SharedPreferences
           if (!blockedSet.contains(phoneNumber) && (phoneNumber.length() == 10))
           {
               blockedSet.add(phoneNumber);
           }
        }
        SharedPrefUtils.setStringSet(getApplicationContext(), SharedPrefUtils.SETTINGS, SharedPrefUtils.BLOCK_LIST, blockedSet);
    }
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        ma.toggle(arg2);
    }

    public  void getAllContacts(ContentResolver cr) {
        allContactsClicked = new HashMap<String,String>();
        Cursor curPhones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);
        while (curPhones.moveToNext())
        {
          String name= curPhones.getString(curPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
          String phoneNumber = toValidPhoneNumber(curPhones.getString(curPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));

           if (!phones.contains(phoneNumber) && (phoneNumber.length() == 10))
            {
                int i = 0;
                while (allContactsClicked.containsKey(name)) // for names that have more than one number
                {
                    name = name + String.valueOf(i);
                    i++;
                }
               names.add(name);
               phones.add(phoneNumber);
               allContactsClicked.put(name,phoneNumber); // helps button selectall to
            }
        }
        curPhones.close();
     }

    class MyAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener//,Filterable
    {  private SparseBooleanArray mCheckStates;
       LayoutInflater mInflater;
        TextView tv1,tv;
        CheckBox cb;

        MyAdapter()
        {
            mCheckStates = new SparseBooleanArray(names.size());
            mInflater = (LayoutInflater)SelectSpecificContacts.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
            View vi = convertView;
            if (convertView == null)
                vi = mInflater.inflate(R.layout.row, null);
            tv = (TextView) vi.findViewById(R.id.textView1);
            tv1 = (TextView) vi.findViewById(R.id.textView2);
            cb = (CheckBox) vi.findViewById(R.id.checkBox);
            tv.setText(names.get(position));
            tv1.setText(phones.get(position));
            cb.setTag(position);
            cb.setOnCheckedChangeListener(this);

            if (blockedSet != null) {
                if (blockedSet.contains((phones.get(position)).toString())) {
                    cb.setChecked(true);
                    ma.notifyDataSetChanged();
                } else
                {
                    cb.setChecked(mCheckStates.get(Integer.valueOf((phones.get(position))), false)); //mcheckstates key is the unique phone number and it defines wether it's checked or not. on the view
                }
            }


            return vi;
        }

        public boolean isChecked(int position) {
                return mCheckStates.get(position, false);
            }

        public void setChecked(int position, boolean isChecked) {
                mCheckStates.put(position, isChecked);
            }

        public void toggle(int position) {
                setChecked(position, !isChecked(position));
            }

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {

            String phoneInIndex = toValidPhoneNumber(phones.get((Integer) buttonView.getTag()).toString());
            String nameInIndex = names.get((Integer) buttonView.getTag()).toString();
            int i = 0;
            while (blockedContacts.containsKey(nameInIndex) && !blockedContacts.containsValue(phoneInIndex)) // for names that have more than one number
            {
                nameInIndex = nameInIndex + String.valueOf(i);
                i++;
            }
            mCheckStates.put(Integer.valueOf(phoneInIndex), isChecked);  //mcheckstates key is the unique phone number and it defines wether it's checked or not. on the view
            if (isChecked && blockedContacts!=null && !blockedContacts.containsValue(phoneInIndex))  // so there won't be any phone duplicate
            {
                blockedContacts.put(nameInIndex , phoneInIndex);
            }
            else if (!isChecked && blockedContacts!=null) {  // if unchecked remove from the black list in the sharedpref
                blockedContacts.remove(nameInIndex);

            if (blockedSet != null)
                if (blockedSet.contains(phoneInIndex))
                {
                blockedSet.remove(phoneInIndex);
                }

            }
        }

    }
}