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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.special.app.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SelectSpecificContacts extends AppCompatActivity implements OnItemClickListener{

    List<String> names = new ArrayList<String>();
    private static final String TAG = SelectSpecificContacts.class.getSimpleName();
  //  List<String> originalnames = new ArrayList<String>();
    List<String> phones = new ArrayList<String>();
    MyAdapter ma ;
    ListView lv;
    Button select;
    // Search EditText
    EditText inputSearch;
   HashMap<String,String> blockedContacts ;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_spec_contacts);

        if (blockedContacts==null)
        {
            blockedContacts = new HashMap<String,String>();

        }

        getAllContacts(this.getContentResolver());
        lv= (ListView) findViewById(R.id.lv);

        ma = new MyAdapter();
        lv.setAdapter(ma);
        lv.setOnItemClickListener(this);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lv.setItemsCanFocus(false);
        lv.setTextFilterEnabled(true);


        Button selectall = (Button) findViewById(R.id.selectall);
        selectall.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                for ( int i=0; i < lv.getCount(); i++) {
                    ma.setChecked(i, true);
                    lv.setItemChecked(i,true);
                    Log.i(TAG, "onclick true");
                }

            }
        });

        Button unselectall = (Button) findViewById(R.id.unselect);
        unselectall.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                for ( int i=0; i < lv.getCount(); i++) {
                    ma.setChecked(i,false);
                    lv.setItemChecked(i,false);
                    Log.i(TAG,"onclick false" );
                }
            }
        });

    }


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


    @Override
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
        super.onPause();
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


        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", blockedContacts);

        if (getParent() == null) {
            setResult(Activity.RESULT_OK, returnIntent);
        } else {
            getParent().setResult(Activity.RESULT_OK, returnIntent);
        }

        super.onBackPressed();
    }



    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
         ma.toggle(arg2);
    }

    public  void getAllContacts(ContentResolver cr) {

        Cursor curPhones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);
        while (curPhones.moveToNext())
        {
          String name=curPhones.getString(curPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
          String phoneNumber = curPhones.getString(curPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
          names.add(name);
          phones.add(phoneNumber);
        }
       // Collections.copy(originalnames,names);

        curPhones.close();
     }

    class MyAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener//,Filterable
    {  private SparseBooleanArray mCheckStates;
       LayoutInflater mInflater;
        TextView tv1,tv;
        CheckBox cb;
       // filter_here filter = new filter_here();
        MyAdapter()
        {
            mCheckStates = new SparseBooleanArray(names.size());
            mInflater = (LayoutInflater)SelectSpecificContacts.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
             vi = mInflater.inflate(R.layout.row, null);
             tv= (TextView) vi.findViewById(R.id.textView1);
             tv1= (TextView) vi.findViewById(R.id.textView2);
             cb = (CheckBox) vi.findViewById(R.id.remove_blocked);
             tv.setText(names.get(position));
             tv1.setText(phones.get(position));
             cb.setTag(position);
            cb.setChecked(mCheckStates.get(position, false));
             cb.setOnCheckedChangeListener(this);
            return vi;
        }
         public boolean isChecked(int position) {
             Log.i(TAG,"isChecked" );
                return mCheckStates.get(position, false);
            }

            public void setChecked(int position, boolean isChecked) {
                Log.i(TAG,"setChecked" );
                mCheckStates.put(position, isChecked);

            }

            public void toggle(int position) {
                Log.i(TAG,"toggle" );
                setChecked(position, !isChecked(position));
            }
        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            Log.i(TAG,"onCheckedChanged" );
             mCheckStates.put((Integer) buttonView.getTag(), isChecked);

            //Toast.makeText(SelectSpecificContacts.this, String.valueOf(isChecked) + "  " + names.get((Integer) buttonView.getTag()).toString() +  phones.get((Integer) buttonView.getTag()).toString() ,Toast.LENGTH_LONG).show();

            if (isChecked && blockedContacts!=null)
                 blockedContacts.put(names.get((Integer) buttonView.getTag()).toString() , phones.get((Integer) buttonView.getTag()).toString() );
            else
                 blockedContacts.remove(names.get((Integer) buttonView.getTag()).toString());

        }

    }
}