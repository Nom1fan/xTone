package com.xtone.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;
import com.xtone.service.IncomingCallService;
import com.xtone.service.logic.CallIdleLogic;
import com.xtone.service.logic.CallIdleLogicImpl;
import com.xtone.service.logic.CallOffHookLogic;
import com.xtone.service.logic.CallOffHookLogicImpl;
import com.xtone.service.logic.CallRingingLogic;
import com.xtone.service.logic.CallRingingLogicImpl;

import java.util.ArrayList;
import java.util.List;

import static android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED;
import static android.telephony.TelephonyManager.EXTRA_INCOMING_NUMBER;

/**
 * Created by Mor on 12/09/2015.
 */
public class IncomingCallReceiver extends BroadcastReceiver {

    private Logger log = LoggerFactory.getLogger();

    private static final String TAG = IncomingCallReceiver.class.getSimpleName();

    private CallIdleLogic callIdleLogic;

    private CallRingingLogic callRingingLogic;

    private CallOffHookLogic callOffHookLogic;

    public IncomingCallReceiver(CallIdleLogic callIdleLogic, CallRingingLogic callRingingLogic, CallOffHookLogic callOffHookLogic, Logger log) {
        this.callIdleLogic = callIdleLogic;
        this.callRingingLogic = callRingingLogic;
        this.callOffHookLogic = callOffHookLogic;
        this.log = log;
    }

    public IncomingCallReceiver() {
        callIdleLogic = new CallIdleLogicImpl();
        callRingingLogic = new CallRingingLogicImpl();
        callOffHookLogic = new CallOffHookLogicImpl();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null || !intent.getAction().equals(ACTION_PHONE_STATE_CHANGED)) {
            return;
        }

        Integer callState = getCallState(context);

        if (callState == null) {
            return;
        }

        String incomingNumber = getIncomingNumber(intent);

        if (incomingNumber == null) {
            return;
        }

        syncOnCallStateChange(context, callState, incomingNumber);
    }

    private String getIncomingNumber(Intent intent) {
        return intent.getStringExtra(EXTRA_INCOMING_NUMBER);
    }

    public void syncOnCallStateChange(Context context, int state, String incomingNumber) {
        log.info(TAG, String.format("Incoming phone number:[%s]", incomingNumber));

        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                callRingingLogic.handle(context, incomingNumber);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                callOffHookLogic.handle(context, incomingNumber);
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                callIdleLogic.handle(context, incomingNumber);
                break;
        }
    }

    private Integer getCallState(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            log.info(TAG, "Getting call state from TelephonyManager");
            return telephonyManager.getCallState();
        } else {
            log.error(TAG, "Failed to get call state. TelephonyManager was null");
            return null;
        }
    }

}
