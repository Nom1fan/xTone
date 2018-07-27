package com.xtone.utils;

import android.content.Context;

import com.xtone.model.MediaFile;

public interface StandOutWindowUtils extends Utility {

    void startStandOutWindow(Context context, String phoneNumber, MediaFile mediaFile);

    void stopStandOutWindow(Context context);
}
