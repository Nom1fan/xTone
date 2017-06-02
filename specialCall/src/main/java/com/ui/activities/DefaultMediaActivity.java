package com.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.enums.SpecialMediaType;
import com.mediacallz.app.R;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by rony on 28/05/17.
 */
public class DefaultMediaActivity extends Activity implements View.OnClickListener {


    private static final String TAG = SelectMediaActivity.class.getSimpleName();

    //region Activity methods (onCreate(), onPause()...)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log(Log.INFO, TAG, "onCreate()");
        setContentView(R.layout.default_media_select_layout);

    }

    @Override
    public void onClick(View v) {

    }
}