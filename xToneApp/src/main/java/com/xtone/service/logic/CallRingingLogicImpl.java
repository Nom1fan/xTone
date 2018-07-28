package com.xtone.service.logic;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;
import com.xtone.model.MediaFile;
import com.xtone.utils.CallSessionUtils;
import com.xtone.utils.ContactsUtils;
import com.xtone.utils.Phone2MediaUtils;
import com.xtone.utils.StandOutWindowUtils;
import com.xtone.utils.UtilsFactory;

public class CallRingingLogicImpl implements CallRingingLogic {

    private static final String TAG = CallRingingLogicImpl.class.getSimpleName();

    private Phone2MediaUtils phone2MediaUtils;

    private StandOutWindowUtils standOutWindowUtils;

    private CallSessionUtils callSessionUtils;

    private Logger log = LoggerFactory.getLogger();

    public CallRingingLogicImpl() {
        phone2MediaUtils = UtilsFactory.instance().getUtility(Phone2MediaUtils.class);
        standOutWindowUtils = UtilsFactory.instance().getUtility(StandOutWindowUtils.class);
        callSessionUtils = UtilsFactory.instance().getUtility(CallSessionUtils.class);
    }

    public CallRingingLogicImpl(Phone2MediaUtils phone2MediaUtils, StandOutWindowUtils standOutWindowUtils, CallSessionUtils callSessionUtils, Logger log) {
        this.phone2MediaUtils = phone2MediaUtils;
        this.standOutWindowUtils = standOutWindowUtils;
        this.callSessionUtils = callSessionUtils;
        this.log = log;
    }

    @Override
    public void handle(Context context, String incomingNumber) {
        log.info(TAG, "Received: CALL_STATE_RINGING");

        if (callSessionUtils.getCallState(context) == TelephonyManager.CALL_STATE_RINGING) {
            log.warn(TAG, "Already in call state ringing. Doing nothing.");
            return;
        }

        callSessionUtils.setCallState(context, TelephonyManager.CALL_STATE_RINGING);

        MediaFile mediaFile = phone2MediaUtils.getMediaFile(context, incomingNumber);
        if (mediaFile == null) {
            log.info(TAG, "No media found. Doing nothing.");
        }

        if (mediaFile != null) {
            log.info(TAG, "Media found!");
            standOutWindowUtils.startStandOutWindow(context, incomingNumber, mediaFile);
        }
    }
}
