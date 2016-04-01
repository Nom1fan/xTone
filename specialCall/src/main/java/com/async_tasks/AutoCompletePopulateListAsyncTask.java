package com.async_tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.AutoCompleteTextView;

import com.data_objects.Contact;
import com.mediacallz.app.R;
import com.ui.components.FilterWithSpaceAdapter;
import com.utils.ContactsUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Populates all contacts
 */
public class AutoCompletePopulateListAsyncTask extends AsyncTask<Context, Integer, FilterWithSpaceAdapter> {

    private ArrayList<String> mPeopleList;
    private final WeakReference<AutoCompleteTextView> autoCompleteTextViewWeakReference;

    public AutoCompletePopulateListAsyncTask(AutoCompleteTextView autoCompleteTextView) {
        autoCompleteTextViewWeakReference = new WeakReference<>(autoCompleteTextView);
    }

    @Override
    protected FilterWithSpaceAdapter doInBackground(Context... contexts) {
        Context context = contexts[0];
        mPeopleList = new ArrayList<>();
        PopulatePeopleList(context);

        return new FilterWithSpaceAdapter(context, R.layout.custcontview, R.id.ccontName, mPeopleList);

    }

    @Override
    protected void onPostExecute(FilterWithSpaceAdapter mWithSpaceAdapter) {
        autoCompleteTextViewWeakReference.get().setAdapter(mWithSpaceAdapter);
    }

    public void PopulatePeopleList(Context context) {

        mPeopleList.clear();
        List<Contact> allContacts = ContactsUtils.getAllContacts(context);

        for(Contact contact: allContacts)
            mPeopleList.add(contact.get_name() + "\n" + contact.get_phoneNumber());

    }

}