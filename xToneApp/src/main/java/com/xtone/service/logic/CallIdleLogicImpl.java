package com.xtone.service.logic;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;
import com.xtone.utils.CallSessionUtils;
import com.xtone.utils.CallSessionUtilsImpl;
import com.xtone.utils.StandOutWindowUtils;
import com.xtone.utils.StandOutWindowUtilsImpl;

public class CallIdleLogicImpl implements CallIdleLogic {

    private static final String TAG = CallIdleLogicImpl.class.getSimpleName();

    private Logger log = LoggerFactory.getLogger();

    private CallSessionUtils callSessionUtils;

    private StandOutWindowUtils standOutWindowUtils;


    public CallIdleLogicImpl() {
        callSessionUtils = new CallSessionUtilsImpl();
        standOutWindowUtils = new StandOutWindowUtilsImpl();
    }

    public CallIdleLogicImpl(Logger log, CallSessionUtils callSessionUtils, StandOutWindowUtils standOutWindowUtils) {
        this.log = log;
        this.callSessionUtils = callSessionUtils;
        this.standOutWindowUtils = standOutWindowUtils;
    }

    @Override
    public void handle(Context context, String incomingNumber) {
        log.info(TAG, "Received:[CALL_STATE_IDLE]");

        int callState = callSessionUtils.getCallState(context);

        if (callState == TelephonyManager.CALL_STATE_RINGING) {
           log.info(TAG, "Call ended.");
           callSessionUtils.setCallState(context, TelephonyManager.CALL_STATE_IDLE);
           standOutWindowUtils.stopStandOutWindow(context);
        }
    }
}
