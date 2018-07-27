package com.xtone.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.xtone.app.R;
import com.xtone.utils.PermissionsUtils;
import com.xtone.utils.UtilsFactory;

import static com.xtone.utils.PermissionsUtils.PERMISSIONS;
import static com.xtone.utils.PermissionsUtils.PERMISSION_REQUEST;

public class MainActivity extends AppCompatActivity {

    private PermissionsUtils permissionsUtils = UtilsFactory.instance().getUtility(PermissionsUtils.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        boolean permissionsSatisfied = permissionsUtils.checkPermissions(this);
        if (!permissionsSatisfied) {
            requestPermissions(PERMISSIONS, PERMISSION_REQUEST);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            boolean permissionGranted = permissionsUtils.isPermissionGranted(grantResults);
            if (!permissionGranted) {
                permissionsUtils.alertAndFinish(this);
            }
        }
    }


}

