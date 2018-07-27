package com.xtone.utils;

import android.content.Context;
import android.widget.Toast;

import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;
import com.xtone.model.MediaFile;

public class StandOutWindowUtilsImpl implements StandOutWindowUtils {

    private static final String TAG = StandOutWindowUtilsImpl.class.getSimpleName();

    private Logger log = LoggerFactory.getLogger();

    @Override
    public void startStandOutWindow(Context context, String phoneNumber, MediaFile mediaFile) {
        Toast.makeText(context, "Showing StandOutwindow...", Toast.LENGTH_LONG);
    }

    @Override
    public void stopStandOutWindow(Context context) {
        Toast.makeText(context, "Closing StandOutwindow...", Toast.LENGTH_LONG);
    }
}
