package com.xtone.service.logic;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;
import com.xtone.utils.CallSessionUtils;
import com.xtone.utils.CallSessionUtilsImpl;
import com.xtone.utils.StandOutWindowUtils;
import com.xtone.utils.StandOutWindowUtilsImpl;

public class CallOffHookLogicImpl implements CallOffHookLogic {

    private static final String TAG = CallOffHookLogicImpl.class.getSimpleName();

    private Logger log = LoggerFactory.getLogger();

    private StandOutWindowUtils standOutWindowUtils;

    private CallSessionUtils callSessionUtils;

    public CallOffHookLogicImpl() {
        standOutWindowUtils = new StandOutWindowUtilsImpl();
        callSessionUtils = new CallSessionUtilsImpl();
    }

    public CallOffHookLogicImpl(Logger log, StandOutWindowUtils standOutWindowUtils, CallSessionUtils callSessionUtils) {
        this.log = log;
        this.standOutWindowUtils = standOutWindowUtils;
        this.callSessionUtils = callSessionUtils;
    }

    @Override
    public void handle(Context context, String incomingNumber) {
        log.info(TAG, "Received: CALL_STATE_OFF_HOOK");

        callSessionUtils.setCallState(context, TelephonyManager.CALL_STATE_OFFHOOK);
        standOutWindowUtils.stopStandOutWindow(context);
    }
}
