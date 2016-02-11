package com.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.special.app.R;
import com.utils.SharedPrefUtils;

import java.util.HashMap;

/**
 * Created by rony on 10/02/2016.
 */


public class BlockMCContacts extends Activity implements View.OnClickListener{

    private static final String TAG = BlockMCContacts.class.getSimpleName();
    private Context _context;
    private RadioButton all_valid;
    private RadioButton contacts_only;
    private RadioButton blacklist_specific;
    private abstract class ActivityRequestCodes {
        public static final int SELECT_BLACK_LIST_CONTACTS = 10;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blocked_user_list);
        _context = getApplicationContext();

        all_valid = (RadioButton) findViewById(R.id.all_valid);
        all_valid.setOnClickListener(this);
        contacts_only = (RadioButton) findViewById(R.id.contacts_only);
        contacts_only.setOnClickListener(this);
        blacklist_specific = (RadioButton) findViewById(R.id.blacklist_specific);
        blacklist_specific.setOnClickListener(this);

        String oldConfig = SharedPrefUtils.getString(_context, SharedPrefUtils.SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME);
        if (oldConfig.isEmpty())
        {
            SharedPrefUtils.setString(_context, SharedPrefUtils.SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME, "ALL");
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

                case "BLOCKLIST":
                    blacklist_specific.setChecked(true);
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.all_valid) {

            SharedPrefUtils.setString(_context, SharedPrefUtils.SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME, "ALL");

        }
        if (id == R.id.contacts_only) {

            SharedPrefUtils.setString(_context, SharedPrefUtils.SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME, "CONTACTS");

        }
        if (id == R.id.blacklist_specific) {

            SharedPrefUtils.setString(_context, SharedPrefUtils.SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME, "BLOCKLIST");

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

                HashMap<String, String> hashMap = (HashMap<String, String>)data.getSerializableExtra("result");
                Toast.makeText(_context, " ACTIVITY RESULT FOR BLOCK LIST HashMapCount: " + String.valueOf(hashMap.size()), Toast.LENGTH_LONG).show();

            }
        }
    }


}
