package com.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.data.objects.ActivityRequestCodes;
import com.data.objects.Contact;
import com.enums.PermissionBlockListLevel;
import com.mediacallz.app.R;
import com.utils.ContactsUtils;
import com.utils.MCBlockListUtils;
import com.utils.SettingsUtils;
import com.utils.UtilityFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.crashlytics.android.Crashlytics.log;
import static com.enums.PermissionBlockListLevel.ALL_VALID;
import static com.enums.PermissionBlockListLevel.BLACK_LIST_SPECIFIC;
import static com.enums.PermissionBlockListLevel.CONTACTS_ONLY;
import static com.enums.PermissionBlockListLevel.EMPTY;
import static com.enums.PermissionBlockListLevel.NO_ONE;

/**
 * Created by rony on 10/02/2016.
 */


public class BlockMCContacts extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = BlockMCContacts.class.getSimpleName();
    private List<String> _namesInListView = new ArrayList<>();
    private List<String> _phonesInListView = new ArrayList<>();
    private List<String> _allContactsNames = new ArrayList<>();
    private List<String> _allContactsPhones = new ArrayList<>();
    private TextView blackListTitle;
    private BlackListAdapter _ma;
    private ListView _lv;
    private Set<String> _blockedContactsSet;

    private final ContactsUtils contactsUtils = UtilityFactory.instance().getUtility(ContactsUtils.class);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blocked_user_list);
        log(Log.INFO,TAG, "onCreate");

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.custom_action_bar);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();
        log(Log.INFO,TAG, "onResume()");
        Context context = getApplicationContext();
        prepareListViewData();

        RadioButton all_valid = (RadioButton) findViewById(R.id.all_valid);
        all_valid.setOnClickListener(this);
        RadioButton contacts_only = (RadioButton) findViewById(R.id.contacts_only);
        contacts_only.setOnClickListener(this);
        RadioButton no_one = (RadioButton) findViewById(R.id.no_one);
        no_one.setOnClickListener(this);
        RadioButton blacklist_specific = (RadioButton) findViewById(R.id.blacklist_specific);
        blacklist_specific.setOnClickListener(this);

        blackListTitle = (TextView) findViewById(R.id.blacklist_title);

        PermissionBlockListLevel oldConfig = SettingsUtils.getWhoCanMCMe(context);
        if (oldConfig.equals(EMPTY)) {
            SettingsUtils.setWhoCanMCMe(context, ALL_VALID);
            all_valid.setChecked(true);
        } else {
            switch (oldConfig) {

                case ALL_VALID:
                    all_valid.setChecked(true);
                    break;

                case CONTACTS_ONLY:
                    contacts_only.setChecked(true);
                    break;

                case NO_ONE:
                    no_one.setChecked(true);
                    break;

                case BLACK_LIST_SPECIFIC:
                    blacklist_specific.setChecked(true);
                    break;
            }
        }

        displayListViewWithNewData();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        Context context = getApplicationContext();
        if (id == R.id.all_valid) {
            SettingsUtils.setWhoCanMCMe(context, ALL_VALID);
        }
        if (id == R.id.contacts_only) {
            SettingsUtils.setWhoCanMCMe(context, CONTACTS_ONLY);
        }
        if (id == R.id.no_one) {
            SettingsUtils.setWhoCanMCMe(context, NO_ONE);
        }
        if (id == R.id.blacklist_specific) {
            SettingsUtils.setWhoCanMCMe(context, BLACK_LIST_SPECIFIC);

            Intent mainIntent = new Intent(BlockMCContacts.this,
                    SelectSpecificContacts.class);
            startActivityForResult(mainIntent, ActivityRequestCodes.SELECT_BLACK_LIST_CONTACTS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == ActivityRequestCodes.SELECT_BLACK_LIST_CONTACTS) {
                prepareListViewData();
                displayListViewWithNewData();
            }
        }
    }

    private void displayListViewWithNewData() {
        _lv = (ListView) findViewById(R.id.blv);
        _lv.removeAllViewsInLayout();
        _ma = new BlackListAdapter();
        _lv.setAdapter(_ma); // clear listview

        populateContactsToDisplayFromBlockedList(_blockedContactsSet);

        _lv.setAdapter(_ma);
        _lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        _lv.setItemsCanFocus(false);
        _lv.setTextFilterEnabled(true);

    }

    private void prepareListViewData() {

        getAllContacts(); // need to get all contacts so we can compare what we have in the sharedpref and pull the name to display in the view
        _namesInListView = new ArrayList<>();
        _phonesInListView = new ArrayList<>();
        _blockedContactsSet = MCBlockListUtils.getBlockListFromShared(getApplicationContext());
    }

    // helps giving the name and phone number to display
    public void populateContactsToDisplayFromBlockedList(Set<String> blockedContactsSet) {

        for (String phone : _allContactsPhones) {
            if (blockedContactsSet.contains(phone) && !_phonesInListView.contains(phone)) {
                _namesInListView.add(_allContactsNames.get(_allContactsPhones.indexOf(phone)));
                _phonesInListView.add(phone);
            }
        }

        String unkownName = getResources().getString(R.string.uknown);
        for (String phone : blockedContactsSet) {
            if (!_phonesInListView.contains(phone)) {
                log(Log.INFO,TAG, " adding phone to black list: " + phone);

                int i = 0;
                while (_namesInListView.contains(unkownName)) // for _namesInListView that have more than one number
                {
                    unkownName = unkownName + String.valueOf(i);
                    i++;
                }

                _namesInListView.add(unkownName);
                _phonesInListView.add(phone);
            }
        }



        if (_phonesInListView.isEmpty())
            blackListTitle.setVisibility(View.INVISIBLE);
        else
            blackListTitle.setVisibility(View.VISIBLE);



    }

    public void getAllContacts() {
        List<Contact> contactsList = contactsUtils.getAllContacts(getApplicationContext());

        for (int i=0; i<contactsList.size(); i++) {
            String name = contactsList.get(i).getContactName();
            String phoneNumber = contactsList.get(i).getPhoneNumber();

            if (!_phonesInListView.contains(phoneNumber) && (phoneNumber.length() == 10)) {
                _allContactsNames.add(name);
                _allContactsPhones.add(phoneNumber);
            }
        }
    }
    class BlackListAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener//,Filterable
    {
        LayoutInflater mInflater;
        TextView blockedContactNumber, blockedContactName;
        Button removeFromBlockedButton;

        BlackListAdapter() {
            mInflater = (LayoutInflater) BlockMCContacts.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
            if (convertView == null)
                vi = mInflater.inflate(R.layout.blacklist_row, null);
            blockedContactName = (TextView) vi.findViewById(R.id.blacklist_name);
            blockedContactNumber = (TextView) vi.findViewById(R.id.blacklist_phone);
            removeFromBlockedButton = (Button) vi.findViewById(R.id.remove_from_blacklist_button);
            blockedContactName.setText(_namesInListView.get(position));
            blockedContactNumber.setText(_phonesInListView.get(position));
            removeFromBlockedButton.setTag(position);
            View.OnClickListener buttonListener = new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    int position = _lv.getPositionForView(v);

                    // Removing contact from view
                    log(Log.INFO,TAG, "position: " + String.valueOf(position));
                    _lv.removeViewInLayout(v);
                    _namesInListView.remove(position);

                    // Removing contact from blocked list
                    log(Log.INFO,TAG, "Remove from Set: " + _phonesInListView.get(position));
                    if (_blockedContactsSet != null)
                        _blockedContactsSet.remove(_phonesInListView.get(position));
                    _phonesInListView.remove(position);
                     MCBlockListUtils.setBlockListFromShared(getApplicationContext(), _blockedContactsSet);
                    _ma.notifyDataSetChanged();
                }
            };
            removeFromBlockedButton.setOnClickListener(buttonListener);
            return vi;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        }
    }

}
