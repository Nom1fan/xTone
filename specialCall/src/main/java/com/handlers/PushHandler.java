package com.handlers;

import android.content.Context;

import com.model.push.PushData;

/**
 * Created by Mor on 12/31/2016.
 */

public interface PushHandler {

    void handlePush(Context ctx, String pushData, Object ... extraParams );
}
