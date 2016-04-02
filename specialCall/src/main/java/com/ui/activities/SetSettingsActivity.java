package com.ui.activities;

import android.app.Activity;
import android.os.Bundle;
/**
 * Created by Mor on 31/03/2016.
 */
public class SetSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new Settings()).commit();
    }

}
