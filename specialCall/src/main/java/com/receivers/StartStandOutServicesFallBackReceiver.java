package com.receivers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.PhoneStateListener;
import android.util.Log;
import android.telephony.TelephonyManager;

import com.data_objects.Constants;
import com.services.IncomingService;
import com.services.OutgoingService;
import com.utils.BroadcastUtils;
import com.utils.PhoneNumberUtils;

/**
 * Created by rony on 01/03/2016.
 */

public class StartStandOutServicesFallBackReceiver extends WakefulBroadcastReceiver {

    public static final int WAIT_FOR_SERVICES_TO_START_IN_MILLI=300;
    private static final String TAG = StartStandOutServicesFallBackReceiver.class.getSimpleName();
    private String mPhoneNumber = "";
    public static String ACTION_START_OUTGOING_SERVICE = "com.receivers.StartStandOutServicesFallBackReceiver.START_OUTGOING_SERVICE";
    public static String INCOMING_PHONE_NUMBER_KEY = "INCOMING_PHONE_NUMBER_KEY";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Log.i(TAG, "ACTION INTENT : " + action);


        registerListenerForPhoneState(context);

        //TODO Rony : get phone number from intent for incoming calls (outgoin calls gives already phonenumber)


        //##INCOMING## Starting service responsible for incoming media callz
        Log.i(TAG, "IncomingService  is Live : " + String.valueOf(IncomingService.isLive));
        if (!IncomingService.isLive)
        {
            Intent incomingServiceIntent = new Intent(context, IncomingService.class);
            incomingServiceIntent.setAction(IncomingService.ACTION_START);

            //if it's incoming call
            if (!mPhoneNumber.isEmpty() && (!intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)))
            {
                Log.i(TAG, " Incoming with number: " + mPhoneNumber);
                incomingServiceIntent.putExtra(Constants.INCOMING_PHONENUMBER_BROADCAST, mPhoneNumber );
                mPhoneNumber = "";
            }
            else
                incomingServiceIntent.putExtra(Constants.INCOMING_PHONENUMBER_BROADCAST, "" );

            Log.i(TAG, " Starting Incoming Service");
            //context.startService(incomingServiceIntent);
            startWakefulService(context, incomingServiceIntent);

        }

        //##OUTGOING## Starting service responsible for Outgoing media callz
        Log.i(TAG, "OutgoingService  is Live : " + String.valueOf(OutgoingService.isLive));
        if (!OutgoingService.isLive)
        {

            Intent outgoingServiceIntent = new Intent(context, OutgoingService.class);
            outgoingServiceIntent.setAction(OutgoingService.ACTION_START);
            Log.i(TAG, " Starting Outgoing Service");
            //context.startService(outgoingServiceIntent);
            startWakefulService(context, outgoingServiceIntent);

            //if it's outgoing call
            if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {

                mPhoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

                final Context ctx = context;
                new Thread(new Runnable() {
                    public void run() {

                        Log.i(TAG, "sleep:" + String.valueOf(WAIT_FOR_SERVICES_TO_START_IN_MILLI));
                        try {
                            Thread.sleep(WAIT_FOR_SERVICES_TO_START_IN_MILLI);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        Log.i(TAG, "is service live to send broadcast: " + String.valueOf(OutgoingService.isLive));
                        //sending service the outgoing call intent again
                        if (OutgoingService.isLive && PhoneNumberUtils.isValidPhoneNumber(mPhoneNumber)) {
                            Intent newIntent = new Intent();
                            newIntent.setAction(ACTION_START_OUTGOING_SERVICE);
                            newIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, mPhoneNumber);
                            BroadcastUtils.sendCustomBroadcast(ctx, TAG, newIntent);
                        }

                    }
                }).start();

            }

        }

    }

    private void registerListenerForPhoneState(Context context) {

        try {

            // TELEPHONY MANAGER class object to register one listner
            TelephonyManager tmgr = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);

            //Create Listner
            MyPhoneStateListener PhoneListener = new MyPhoneStateListener();

            // Register listener for LISTEN_CALL_STATE
            tmgr.listen(PhoneListener, PhoneStateListener.LISTEN_CALL_STATE);

        } catch (Exception e) {
            Log.e("Phone Receive Error", " " + e);
        }

    }

    private class MyPhoneStateListener extends PhoneStateListener {

        public void onCallStateChanged(int state, String incomingNumber) {

            if (!incomingNumber.isEmpty())
                mPhoneNumber = PhoneNumberUtils.toValidPhoneNumber(incomingNumber);
        }
    }

}