package com.ui.components;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.widget.AutoCompleteTextView;
import android.widget.SimpleAdapter;

import com.special.app.R;
import com.utils.FilterWithSpaceAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// ASYNC TASK To Populate all contacts , it can take long time and it delays the UI
public class AutoCompletePopulateListAsyncTask extends AsyncTask<Void, Integer, FilterWithSpaceAdapter> {

  //  private ArrayList<Map<String, String>> mPeopleList ;
    private ArrayList<String> mPeopleList ;
    private final WeakReference<AutoCompleteTextView> autoCompleteTextViewWeakReference;
    private final Context mContext;
  //  private SimpleAdapter mAdapter;
    private FilterWithSpaceAdapter mWithSpaceAdapter;

    private static final String[] PROJECTION = new String[] {
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
            // ContactsContract.CommonDataKinds.Phone.TYPE

    };

    public AutoCompletePopulateListAsyncTask(Context context ,AutoCompleteTextView autoCompleteTextView) {
        autoCompleteTextViewWeakReference = new WeakReference<>(autoCompleteTextView);
        mContext = context;
    }

    @Override
    protected FilterWithSpaceAdapter doInBackground(Void... params) {
        mPeopleList = new ArrayList<>();
        PopulatePeopleList();
    //    mAdapter = new SimpleAdapter(mContext, mPeopleList, R.layout.custcontview ,new String[] { "Name", "Phone" /*, "Type"*/ }, new int[] { R.id.ccontName, R.id.ccontNo/*, R.id.ccontType*/ });
        mWithSpaceAdapter = new FilterWithSpaceAdapter(mContext,R.layout.custcontview,R.id.ccontName ,mPeopleList);

        return mWithSpaceAdapter;

    }

    @Override
    protected void onPostExecute(FilterWithSpaceAdapter mWithSpaceAdapter) {

       autoCompleteTextViewWeakReference.get().setAdapter(mWithSpaceAdapter);

    }

    public void PopulatePeopleList()
    {

        mPeopleList.clear();
        Cursor people = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION, null, null, null);

        if (people != null) {
            try {
                final int displayNameIndex = people.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                final int phonesIndex = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                // final int phoneTypeIndex = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);

                String contactName, phoneNumber, numberType;

                while (people.moveToNext()) {

                    contactName = people.getString(displayNameIndex);
                    phoneNumber = people.getString(phonesIndex);
                    // numberType = people.getString(phoneTypeIndex);

                  //  Map<String, String> NamePhoneType = new HashMap<>();

                  //  NamePhoneType.put("Name", contactName);
                  //  NamePhoneType.put("Phone", phoneNumber);

                   /* if (numberType.equals("0"))
                        NamePhoneType.put("Type", "Work");
                    else if (numberType.equals("1"))
                        NamePhoneType.put("Type", "Home");
                    else if (numberType.equals("2"))
                        NamePhoneType.put("Type", "Mobile");
                    else
                        NamePhoneType.put("Type", "Other");*/

                    //Then add this map to the list.
                    mPeopleList.add(contactName + "\n" +phoneNumber);
                }

            } finally {
                people.close();
            }

        }


    }

}