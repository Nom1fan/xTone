package com_international.handlers.select_media_activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com_international.handlers.Handler;
import com_international.ui.activities.PreviewMediaActivity;
import com_international.ui.activities.SelectMediaActivity;

import com_international.enums.SpecialMediaType;
import com_international.files.media.MediaFile;

/**
 * Created by Mor on 16/07/2016.
 */
public class ActivityRequestPreviewMediaResultHandler implements Handler {

    @Override
    public void handle(Context ctx, Object... params) {
        Intent data = (Intent) params[0];
        SelectMediaActivity selectMediaActivity = (SelectMediaActivity) params[1];
        SpecialMediaType specialMediaType = (SpecialMediaType) params[2];

        MediaFile resultFile = (MediaFile) data.getSerializableExtra(PreviewMediaActivity.RESULT_FILE);
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
