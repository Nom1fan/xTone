package com.handlers.push.service;

import android.content.Context;
import android.content.Intent;

import com.data.objects.ClearMediaData;
import com.handlers.AbstractPushHandler;
import com.services.ClearMediaIntentService;

/**
 * Created by Mor on 12/31/2016.
 */

public class PushClearMediaHandler extends AbstractPushHandler {

    public static final String TAG = PushClearMediaHandler.class.getSimpleName();

    @Override
    public void handlePush(Context ctx, String pushData, Object... extraParams) {
        ClearMediaData clearMediaData = gson.fromJson(pushData, ClearMediaData.class);
        Intent i = new Intent(ctx, ClearMediaIntentService.class);
        i.putExtra(ClearMediaIntentService.CLEAR_MEDIA_DATA, clearMediaData);
        ctx.startService(i);
    }
}
