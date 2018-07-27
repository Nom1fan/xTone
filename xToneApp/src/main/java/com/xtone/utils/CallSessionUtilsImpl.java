package com.xtone.utils;

import android.content.Context;

import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;

public class CallSessionUtilsImpl implements CallSessionUtils {

    private static final String TAG = CallSessionUtilsImpl.class.getSimpleName();

    private SharedPrefUtils sharedPrefUtils = UtilsFactory.instance().getUtility(SharedPrefUtils.class);

    private Logger log = LoggerFactory.getLogger();


    @Override
    public int getCallState(Context context) {
        return sharedPrefUtils.getInt(context, "CALL_SESSION", "CALL_STATE");
    }

    @Override
    public void setCallState(Context context, int callState) {
        sharedPrefUtils.setInt(context, "CALL_SESSION", "CALL_STATE", callState);
    }
}
