package com_international.handlers;

import android.content.Context;

/**
 * Created by Mor on 12/31/2016.
 */

public interface PushHandler {

    void handlePush(Context ctx, String pushData, Object ... extraParams );
}
