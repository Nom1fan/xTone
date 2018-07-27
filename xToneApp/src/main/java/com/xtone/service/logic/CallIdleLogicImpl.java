package com.xtone.service.logic;

import android.content.Context;

import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;

public class CallIdleLogicImpl implements CallIdleLogic {

    private static final String TAG = CallIdleLogicImpl.class.getSimpleName();

    private static final Logger log = LoggerFactory.getLogger();

    @Override
    public void handle(Context context, String incomingNumber) {
        log.info(TAG, "Received: CALL_STATE_IDLE");
    }
}
