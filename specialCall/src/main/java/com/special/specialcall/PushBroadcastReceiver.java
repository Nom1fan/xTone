package com.special.specialcall;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.PowerManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.android.services.ServerProxy;
import com.parse.ParsePushBroadcastReceiver;

/**
 * Created by mor on 10/09/2015.
 */
public class PushBroadcastReceiver extends ParsePushBroadcastReceiver {

    private static final String TAG = PushBroadcastReceiver.class.getSimpleName();

    @Override
    protected void onPushReceive(Context context, Intent intent) {

        callInfoToast(context, "Push notification received", Color.RED);
        Log.i(TAG, "Push notification received");

        Intent i = new Intent(context.getApplicationContext(), ServerProxy.class);
        context.startService(i);


    }

    private void callInfoToast(Context context, final String text, final int g) {

        Toast toast = Toast.makeText(context, text,
                Toast.LENGTH_LONG);
        TextView v = (TextView) toast.getView().findViewById(
                android.R.id.message);
        v.setTextColor(g);
        toast.show();
    }

}
