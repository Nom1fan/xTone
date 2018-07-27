package com.xtone.service.logic;

import android.content.Context;

import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;

public class CallOffHookLogicImpl implements CallOffHookLogic {

    private static final String TAG = CallOffHookLogicImpl.class.getSimpleName();

    private static final Logger log = LoggerFactory.getLogger();

    @Override
    public void handle(Context context, String incomingNumber) {
        log.info(TAG, "Received: CALL_STATE_OFF_HOOK");
    }
}
