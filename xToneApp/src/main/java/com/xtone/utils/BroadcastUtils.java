package com.xtone.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;

import java.util.Map;

/**
 * Created by Mor on 15/10/2015.
 */
public class BroadcastUtils implements Utility {

    private static final Logger log = LoggerFactory.getLogger();

    private StringUtils stringUtils;

    public BroadcastUtils() {
        this.stringUtils = UtilsFactory.instance().getUtility(StringUtils.class);
    }

    public void sendCustomBroadcast(Context context, String tag, String action, Bundle bundle) {
        log.info(tag, String.format("Sending broadcast. Action[:%s], Params:[%s]", action, stringUtils.toString(bundle)));

        Intent i = new Intent(action);
        i.putExtra("bundle", bundle);

        context.sendBroadcast(i);
    }
}
