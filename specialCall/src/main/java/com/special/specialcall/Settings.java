package com.special.specialcall;

import java.util.ArrayList;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.ContactsContract;
import android.widget.TextView;
import android.widget.Toast;
public class Settings extends PreferenceActivity {
	
//	private ArrayList<Contact> allowedContacts = new ArrayList<Contact>();
    private abstract class ActivityRequestCodes {
   	 
    	 public static final int SELECT_CONTACT_PREFS = 1;
   	 
    }
	
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.settings);
        
        Preference button = (Preference)findPreference("select_spec_contacts_btn");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference arg0) { 
                        	Intent i = new Intent();
        					i.setClass(getApplicationContext(), SelectSpecificContacts.class);
        					
        					startActivity(i);    
                            return true;
                        }
                    });
        
    }
    
    // Maybe unnecessary due to new SelectSpecificContacts Class
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	if(requestCode == ActivityRequestCodes.SELECT_CONTACT_PREFS)
        {
       	 try {
           	 if (data != null) {
           	        Uri uri = data.getData();

           	        if (uri != null) {
           	            Cursor c = null;
           	            try {
           	                c = getContentResolver().query(uri, new String[]{ 
           	                            ContactsContract.CommonDataKinds.Phone.NUMBER,  
           	                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, },
           	                        null, null, null);

           	                if (c != null && c.moveToFirst()) {
           	                    String number = c.getString(0);
           	                    String name = c.getString(1);		
           	                    
           	              //      allowedContacts.add(new Contact(name,number));
           	                    String str = "";
           	             //       for(Contact contact : allowedContacts)
           	            //        {
           	            //        	str+="Name:"+contact.get_name()+
           	            //        				" Number:"+contact.get_number()+"\n";
           	            //        }
           	                    callInfoToast(str);
           	                    	                	                    
           	                }
           	            } finally {
           	                if (c != null) {
           	                    c.close();
           	                }
           	            }
           	        }
           	 } 
           	 else		
           	 {
           		 callErrToast("SELECT_CONTACT_PREFS: data is null");
           		 throw new Exception("SELECT_CONTACT_PREFS: data is null");
           	 }
   	 	} catch (Exception e) {
   	 		e.printStackTrace();
   	 	}
			
    	}

    }
    
	private void callErrToast(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
				TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
				v.setTextColor(Color.RED);
				toast.show();
			}				
		});					
	}		
	
	private void callInfoToast(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {					
				Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
				TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
				v.setTextColor(Color.GREEN);
				toast.show();
			}				
		});
	}
 }
    
    
