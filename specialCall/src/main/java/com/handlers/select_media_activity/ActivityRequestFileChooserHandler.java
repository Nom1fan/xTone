package com.handlers.select_media_activity;

import android.content.Context;
import android.content.Intent;

import com.ui.activities.SelectMediaActivity;

/**
 * Created by Mor on 16/07/2016.
 */
public class ActivityRequestFileChooserHandler extends ActivityRequestBeforePreviewHandler {

    @Override
    public void handle(Context ctx, Object... params) {
        TAG = ActivityRequestFileChooserHandler.class.getSimpleName();
        Intent data = (Intent) params[0];
        this.selectMediaActivity = (SelectMediaActivity) params[1];
        startPreviewActivity(ctx, data, false);

    }
}
