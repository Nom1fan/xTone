package com.receivers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.app.AppStateManager;
import com.crashlytics.android.Crashlytics;
import com.services.IncomingService;
import com.services.OutgoingService;
import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;

import utils.PhoneNumberUtils;

/**
 * Created by rony on 01/03/2016.
 */

public class StartStandOutServicesFallBackReceiver extends WakefulBroadcastReceiver {

    public static final int WAIT_FOR_SERVICES_TO_START_IN_MILLI=300;
    private static final String TAG = StartStandOutServicesFallBackReceiver.class.getSimpleName();
    private static String outgoingPhoneNumber="";
    public final static String ACTION_START_OUTGOING_SERVICE = "com.receivers.StartStandOutServicesFallBackReceiver.START_OUTGOING_SERVICE";
    public final static String INCOMING_PHONE_NUMBER_KEY = "INCOMING_PHONE_NUMBER_KEY";
    public final static String WAKEFUL_INTENT = "WAKEFUL_INTENT";


    @Override
    public void onReceive(Context context, Intent intent) {
        if (AppStateManager.isLoggedIn(context)) {  // make sure the services won't start on Login
        String action = intent.getAction();
        Crashlytics.log(Log.INFO,TAG, "onReceive ACTION INTENT : " + action);

            if (action != null) {
                String state1 = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                Log.d(TAG, " PhoneStateReceiver**Call State=" + state1);

            }
            // Sending an incoming phone number for incoming service
            if (action != null && !IncomingService.isLive) // TODO check if Alarm sends action as null or intent. and make general if
                if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
                    String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    Log.d(TAG, "IncomingServiceFallBack PhoneStateReceiver**Call State=" + state);

                    if (state.equals(TelephonyManager.EXTRA_STATE_RINGING) && !SharedPrefUtils.getBoolean(context, SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_RINGING_SESSION)) {
                        try {
                            Thread.sleep(1000);  // TODO REMOVE SLEEP !! or decide a better technique
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // Incoming call
                        String incomingNumber = PhoneNumberUtils.toValidLocalPhoneNumber(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));

                        Intent incomingServiceIntent = new Intent(context, IncomingService.class);
                        incomingServiceIntent.setAction(IncomingService.ACTION_START);

                        if (incomingNumber != null) // unidentified caller
                            Crashlytics.log(Log.INFO,TAG, "IncomingServiceFallBack  EXTRA_INCOMING_NUMBER : " + incomingNumber);

                        //if it's incoming call
                        if (incomingNumber != null)
                            if (!incomingNumber.isEmpty()) {
                                Crashlytics.log(Log.INFO,TAG, "IncomingServiceFallBack putExtra Incoming with number: " + incomingNumber);
                                incomingServiceIntent.putExtra(INCOMING_PHONE_NUMBER_KEY, incomingNumber);
                            } else
                                incomingServiceIntent.putExtra(INCOMING_PHONE_NUMBER_KEY, "");

                        Crashlytics.log(Log.INFO,TAG, " Starting Incoming Service");
                        incomingServiceIntent.putExtra(WAKEFUL_INTENT, true);
                        startWakefulService(context, incomingServiceIntent);

                    }

                }

        // Starting service responsible for incoming media callz
        Crashlytics.log(Log.INFO,TAG, "IncomingService  is Live : " + String.valueOf(IncomingService.isLive));
        if (!IncomingService.isLive) {

            Intent incomingServiceIntent = new Intent(context, IncomingService.class);
            incomingServiceIntent.setAction(IncomingService.ACTION_START);
            Crashlytics.log(Log.INFO,TAG, " Starting Incoming Service");
            incomingServiceIntent.putExtra(WAKEFUL_INTENT, true);
            startWakefulService(context, incomingServiceIntent);

        }

        //region outgoing service
        // Starting service responsible for outgoing media callz
        Crashlytics.log(Log.INFO,TAG, "OutgoingService  is Live : " + String.valueOf(OutgoingService.isLive));
        if (!OutgoingService.isLive) {

            Intent outgoingServiceIntent = new Intent(context, OutgoingService.class);
            outgoingServiceIntent.setAction(OutgoingService.ACTION_START);
            outgoingServiceIntent.putExtra(WAKEFUL_INTENT, true);
            Crashlytics.log(Log.INFO,TAG, " Starting Outgoing Service");
            startWakefulService(context, outgoingServiceIntent);

            //if it's outgoing call
            if (action != null) // TODO check if Alarm sends action as null or intent. and make general if
                if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {

                    outgoingPhoneNumber = PhoneNumberUtils.toValidLocalPhoneNumber(intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));

                    final Context ctx = context;
                    new Thread(new Runnable() {
                        public void run() {

                            Crashlytics.log(Log.INFO,TAG, "sleep:" + String.valueOf(WAIT_FOR_SERVICES_TO_START_IN_MILLI));
                            try {
                                Thread.sleep(WAIT_FOR_SERVICES_TO_START_IN_MILLI);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            Crashlytics.log(Log.INFO,TAG, "is service live to send broadcast: " + String.valueOf(OutgoingService.isLive));
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
        //endregion


    }

    }


}