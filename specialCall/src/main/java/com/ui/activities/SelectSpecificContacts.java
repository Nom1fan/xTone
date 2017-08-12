package com.ui.activities;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

import com.data.objects.Contact;
import com.mediacallz.app.R;
import com.utils.ContactsUtils;
import com.utils.MCBlockListUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.utils.PhoneNumberUtils;
import com.utils.UtilityFactory;

import static com.crashlytics.android.Crashlytics.log;

public class SelectSpecificContacts extends AppCompatActivity implements OnItemClickListener {

    private static final String TAG = SelectSpecificContacts.class.getSimpleName();
    private List<String> _namesInListView = new ArrayList<>(); // the list that the adapter uses to populate the view
    private List<String> _phonesInListView = new ArrayList<>(); // the list that the adapter uses to populate the view
    private BlackListAdapter _ma;
    private ListView _lv;
    private Set<String> _blockedSet = new HashSet<>();
    private HashMap<String, String> _allContacts;
    private final ContactsUtils contactsUtils = UtilityFactory.instance().getUtility(ContactsUtils.class);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_spec_contacts);
        log(Log.INFO,TAG, "onCreate");

    }

    @Override
    protected void onResume() {
        super.onResume();
        log(Log.INFO,TAG, "onResume()");


        prepareListViewData();
        prepareListView();
        displayListViewWithNewData();

        prepareSelectAllButton();
        prepareUnSelectAllButton();
        prepareBackButton();
    }

    private void prepareBackButton() {
        Button back = (Button) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log(Log.INFO,TAG, "Back Button Pressed");
                returnWithResultIntent();
                finish();
            }
        });
    }

    private void prepareSelectAllButton() {
        Button selectall = (Button) findViewById(R.id.selectall);
        selectall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < _phonesInListView.size(); i++) {
                    _ma.setChecked(i, true);
                    _lv.setItemChecked(i, true);
                }
                saveBlockedContacts(_allContacts); // Save All contacts to sharedPref
                _ma.notifyDataSetChanged();
            }
        });
    }

    private void prepareUnSelectAllButton() {

        Button unselectall = (Button) findViewById(R.id.unselect);
        unselectall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                for (int i = 0; i < _phonesInListView.size(); i++) {
                    _ma.setChecked(i, false);
                    _lv.setItemChecked(i, false);
                }
                displayListViewWithNewData();
                _ma.notifyDataSetChanged();

                _blockedSet = new HashSet<>();
                MCBlockListUtils.setBlockListFromShared(getApplicationContext(), _blockedSet);
            }
        });


    }

    private void prepareListViewData() {
        _blockedSet = MCBlockListUtils.getBlockListFromShared(getApplicationContext());
        populateContactsToDisplayFromBlockedList(); // populate all contacts to view with checkboxes
    }

    private void displayListViewWithNewData() {
        _ma = new BlackListAdapter();
        _lv.setAdapter(_ma);  // link the listview with the adapter
    }

    private void prepareListView() {
        _lv = (ListView) findViewById(R.id.lv);
        _lv.setOnItemClickListener(this);
        _lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        _lv.setItemsCanFocus(false);
        _lv.setTextFilterEnabled(true);
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
                while (position < _namesInListView.size() - 1) {
                    if (_namesInListView.get(position).toUpperCase().contains(s.toUpperCase())) {
                        _lv.smoothScrollToPositionFromTop(position, 0, 200);
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
        log(Log.INFO,TAG, "onPause");
        returnWithResultIntent();
        super.onPause();
    }

    private void returnWithResultIntent() {
        Intent returnIntent = new Intent();

        if (getParent() == null) {
            setResult(Activity.RESULT_OK, returnIntent);
        } else {
            getParent().setResult(Activity.RESULT_OK, returnIntent);
        }
    }

    @Override
    public void onBackPressed() {
        log(Log.INFO,TAG, "onBackPressed");
        returnWithResultIntent();
        super.onBackPressed();
    }

    // saving black listed contacts to SharedPref
    private void saveBlockedContacts(HashMap<String, String> contactsMap) {
        Iterator contactIterator = contactsMap.keySet().iterator();
        _blockedSet = new HashSet<>();
        while (contactIterator.hasNext()) {
            String name = (String) contactIterator.next();
            String phoneNumber = contactsMap.get(name);
            // ADDING  To Black List is SharedPreferences
            if (!_blockedSet.contains(phoneNumber) && PhoneNumberUtils.isValidPhoneNumber(phoneNumber))
            {
                _blockedSet.add(phoneNumber);
            }
        }
        MCBlockListUtils.setBlockListFromShared(getApplicationContext(), _blockedSet);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        _ma.toggle(arg2);
    }

    public void populateContactsToDisplayFromBlockedList() {
        _allContacts = new HashMap<>();

        List<Contact> contactsList = contactsUtils.getAllContacts(getApplicationContext());

        for (int x=0; x<contactsList.size(); x++) {
            String name = contactsList.get(x).getContactName();
            String phoneNumber = contactsList.get(x).getPhoneNumber();

            if (!_phonesInListView.contains(phoneNumber) && PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) // so there won't be any phone duplicates
            {
                int i = 0;
                while (_allContacts.containsKey(name)) // for namesInListView that have more than one number
                {
                    name = name + String.valueOf(i);
                    i++;
                }
                _namesInListView.add(name);
                _phonesInListView.add(phoneNumber);
                _allContacts.put(name, phoneNumber); // helps button selectall to
            }
        }

        // Handling Numbers That Are Not stored in Native Contacts
        String unkownName =getResources().getString(R.string.uknown);
        for (String phone : _blockedSet) {
            if (!_phonesInListView.contains(phone) && PhoneNumberUtils.isValidPhoneNumber(phone))
            {
                int i = 0;
                while (_allContacts.containsKey(unkownName)) // for namesInListView that have more than one number
                {
                    unkownName = unkownName + String.valueOf(i);
                    i++;
                }

                _namesInListView.add(unkownName);
                _phonesInListView.add(phone);
                _allContacts.put(unkownName, phone);
                log(Log.INFO,TAG, " adding phone to black list: " + phone);
            }
        }
    }

    class BlackListAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener//,Filterable
    {
        private SparseBooleanArray mCheckStates;
        LayoutInflater mInflater;
        TextView tv1, tv;
        CheckBox cb;

        BlackListAdapter() {
            mCheckStates = new SparseBooleanArray(_namesInListView.size());
            mInflater = (LayoutInflater) SelectSpecificContacts.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return _namesInListView.size();
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
            if (vi == null)
                vi = LayoutInflater.from(getApplicationContext()).inflate(R.layout.row, null);  // vi = mInflater.inflate(R.layout.row, null);

            tv = (TextView) vi.findViewById(R.id.textView1);
            tv1 = (TextView) vi.findViewById(R.id.textView2);
            cb = (CheckBox) vi.findViewById(R.id.checkBox);

            if (_namesInListView.get(position) != null)
                tv.setText(_namesInListView.get(position));

            if (_phonesInListView.get(position) != null)
                tv1.setText(_phonesInListView.get(position));

            cb.setTag(position);
            cb.setOnCheckedChangeListener(this);

            if (_blockedSet != null) {
                if (_blockedSet.contains((_phonesInListView.get(position)))) {
                    cb.setChecked(true);
                    _ma.notifyDataSetChanged();

                } else {

                    try {
                        if (_phonesInListView.get(position) != null)
                            cb.setChecked(mCheckStates.get(Integer.valueOf((_phonesInListView.get(position))), false)); //mcheckstates key is the unique phone number and it defines wether it's checked or not. on the view
                    } catch (Exception e) {
                        log(Log.ERROR,TAG, "listview can't block OR show phone on listview: " + (_phonesInListView.get(position)) + " " + _namesInListView.get(position));

                    }

                    _ma.notifyDataSetChanged();
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

            String phoneInIndex = PhoneNumberUtils.toValidLocalPhoneNumber(_phonesInListView.get((Integer) buttonView.getTag()));
            String nameInIndex = _namesInListView.get((Integer) buttonView.getTag());

            try {
                mCheckStates.put(Integer.valueOf(phoneInIndex), isChecked);  //mcheckstates key is the unique phone number and it defines wether it's checked or not. on the view
            } catch (Exception e) {
                log(Log.ERROR,TAG, "listview can't select checkbox phone on listview: " + Integer.valueOf(phoneInIndex) + " " + nameInIndex);

            }
            if (isChecked)  // so there won't be any phone duplicate
            {
                if (_blockedSet != null) {
                    _blockedSet.add(phoneInIndex);
                  MCBlockListUtils.setBlockListFromShared(getApplicationContext(), _blockedSet);
                }
            } else {  // if unchecked remove from the black list in the sharedpref

                if (_blockedSet != null)
                    if (_blockedSet.contains(phoneInIndex)) {
                        _blockedSet.remove(phoneInIndex);
                       MCBlockListUtils.setBlockListFromShared(getApplicationContext(), _blockedSet);
                    }

            }
        }

    }
}