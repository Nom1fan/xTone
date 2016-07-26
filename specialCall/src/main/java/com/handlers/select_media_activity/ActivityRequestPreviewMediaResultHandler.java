package com.handlers.select_media_activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.data_objects.ActivityRequestCodes;
import com.handlers.Handler;
import com.ui.activities.PreviewMediaActivity;
import com.ui.activities.SelectMediaActivity;

import DataObjects.SpecialMediaType;
import FilesManager.FileManager;

/**
 * Created by Mor on 16/07/2016.
 */
public class ActivityRequestPreviewMediaResultHandler implements Handler {

    @Override
    public void handle(Context ctx, Object... params) {
        Intent data = (Intent) params[0];
        SelectMediaActivity selectMediaActivity = (SelectMediaActivity) params[1];
        SpecialMediaType specialMediaType = null;
        int sMTypeCode = data.getIntExtra(SelectMediaActivity.SPECIAL_MEDIA_TYPE, 1);
        if (sMTypeCode == ActivityRequestCodes.SELECT_CALLER_MEDIA) {
            specialMediaType = SpecialMediaType.CALLER_MEDIA;
        } else if (sMTypeCode == ActivityRequestCodes.SELECT_PROFILE_MEDIA) {
            specialMediaType = SpecialMediaType.PROFILE_MEDIA;
        }

        FileManager resultFile = (FileManager) data.getSerializableExtra(PreviewMediaActivity.RESULT_FILE);
        Intent resultIntent = new Intent();

        resultIntent.putExtra(SelectMediaActivity.RESULT_SPECIAL_MEDIA_TYPE, specialMediaType);
        resultIntent.putExtra(SelectMediaActivity.RESULT_FILE, resultFile);

        if (selectMediaActivity.getParent() == null) {
            selectMediaActivity.setResult(Activity.RESULT_OK, resultIntent);
        } else {
            selectMediaActivity.getParent().setResult(Activity.RESULT_OK, resultIntent);
        }

        selectMediaActivity.finish();
    }
}
