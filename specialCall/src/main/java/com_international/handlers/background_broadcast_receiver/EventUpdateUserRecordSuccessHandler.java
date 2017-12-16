    package com_international.handlers.background_broadcast_receiver;

import android.content.Context;
import android.os.Build;

import com_international.data.objects.Constants;
import com_international.handlers.Handler;

    /**
 * Created by Mor on 16/07/2016.
 */
public class EventUpdateUserRecordSuccessHandler implements Handler {

    private static final String TAG = EventUpdateUserRecordSuccessHandler.class.getSimpleName();

    @Override
    public void handle(Context ctx, Object... params) {
        Constants.MY_ANDROID_VERSION(ctx, Build.VERSION.RELEASE);
    }
}
