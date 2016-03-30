package com.receivers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.app.AppStateManager;
import com.services.IncomingService;
import com.services.OutgoingService;
import com.utils.BroadcastUtils;

import utils.PhoneNumberUtils;

/**
 * Created by rony on 01/03/2016.
 */

public class StartStandOutServicesFallBackReceiver extends WakefulBroadcastReceiver {

    public static final int WAIT_FOR_SERVICES_TO_START_IN_MILLI=300;
    private static final String TAG = StartStandOutServicesFallBackReceiver.class.getSimpleName();
    private static String outgoingPhoneNumber="";
    public static String ACTION_START_OUTGOING_SERVICE = "com.receivers.StartStandOutServicesFallBackReceiver.START_OUTGOING_SERVICE";
    public static String INCOMING_PHONE_NUMBER_KEY = "INCOMING_PHONE_NUMBER_KEY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!AppStateManager.getAppState(context).equals(AppStateManager.STATE_LOGGED_OUT)) {  // make sure the services won't start on Login
        String action = intent.getAction();
        Log.i(TAG, "onReceive ACTION INTENT : " + action);


        // Starting service responsible for incoming media callz
        Log.i(TAG, "IncomingService  is Live : " + String.valueOf(IncomingService.isLive));
        if (!IncomingService.isLive) {

            Intent incomingServiceIntent = new Intent(context, IncomingService.class);
            incomingServiceIntent.setAction(IncomingService.ACTION_START);
            Log.i(TAG, " Starting Incoming Service");
            startWakefulService(context, incomingServiceIntent);

        }


        // Starting service responsible for outgoing media callz
        Log.i(TAG, "OutgoingService  is Live : " + String.valueOf(OutgoingService.isLive));
        if (!OutgoingService.isLive) {

            Intent outgoingServiceIntent = new Intent(context, OutgoingService.class);
            outgoingServiceIntent.setAction(OutgoingService.ACTION_START);
            Log.i(TAG, " Starting Outgoing Service");
            startWakefulService(context, outgoingServiceIntent);

            //if it's outgoing call
            if (intent != null && action != null) // TODO check if Alarm sends action as null or intent. and make general if
                if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {

                    outgoingPhoneNumber = PhoneNumberUtils.toValidLocalPhoneNumber(intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));

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
                            if (OutgoingService.isLive && PhoneNumberUtils.isValidPhoneNumber(outgoingPhoneNumber)) {
                                Intent newIntent = new Intent();
                                newIntent.setAction(ACTION_START_OUTGOING_SERVICE);
                                newIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, outgoingPhoneNumber);
                                BroadcastUtils.sendCustomBroadcast(ctx, TAG, newIntent);
                            }

                        }
                    }).start();

                }

        }

        // Sending an incoming phone number for incoming service
        if (intent != null && action != null) // TODO check if Alarm sends action as null or intent. and make general if
            if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                Log.d(TAG, "IncomingServiceFallBack PhoneStateReceiver**Call State=" + state);

                if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    try {
                        Thread.sleep(1000, 0);  // TODO REMOVE SLEEP !! or decide a better technique
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Incoming call
                    String incomingNumber = PhoneNumberUtils.toValidLocalPhoneNumber(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));

                    Intent incomingServiceIntent = new Intent(context, IncomingService.class);
                    incomingServiceIntent.setAction(IncomingService.ACTION_START);

                    if (incomingNumber != null) // unidentified caller
                        Log.i(TAG, "IncomingServiceFallBack  EXTRA_INCOMING_NUMBER : " + incomingNumber);

                    //if it's incoming call
                    if (incomingNumber != null)
                        if (!incomingNumber.isEmpty()) {
                            Log.i(TAG, "IncomingServiceFallBack putExtra Incoming with number: " + incomingNumber);
                            incomingServiceIntent.putExtra(INCOMING_PHONE_NUMBER_KEY, incomingNumber);
                        } else
                            incomingServiceIntent.putExtra(INCOMING_PHONE_NUMBER_KEY, "");

                    Log.i(TAG, " Starting Incoming Service");
                    startWakefulService(context, incomingServiceIntent);

                }

            }
    }

    }


}