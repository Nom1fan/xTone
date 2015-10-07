package com.receivers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParsePushBroadcastReceiver;

/**
 * Created by mor on 10/09/2015.
 */
public class PushBroadcastReceiver extends ParsePushBroadcastReceiver {

    private static final String TAG = PushBroadcastReceiver.class.getSimpleName();

    @Override
    protected void onPushReceive(Context context, Intent intent) {

        //callInfoToast(context, "Push notification received", Color.RED);
        Log.i(TAG, "Push notification received");

//        Intent i = new Intent(context.getApplicationContext(), ServerProxyService.class);
//        context.startService(i);


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
