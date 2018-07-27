package com.xtone.service.logic;

import android.content.Context;

import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;

public class CallRingingLogicImpl implements CallRingingLogic {

    private static final String TAG = CallRingingLogicImpl.class.getSimpleName();

    private static final Logger log = LoggerFactory.getLogger();

    @Override
    public void handle(Context context, String incomingNumber) {
        log.info(TAG, "Received: CALL_STATE_RINGING");
    }
}
