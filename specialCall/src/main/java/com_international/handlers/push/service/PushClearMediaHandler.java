package com_international.handlers.push.service;

import android.content.Context;
import android.content.Intent;

import com_international.data.objects.ClearMediaData;
import com_international.handlers.AbstractPushHandler;
import com_international.services.ClearMediaIntentService;

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
