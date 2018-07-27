package com.xtone.utils;

import android.content.Context;

import com.xtone.model.MediaFile;

public interface CallSessionUtils extends Utility {

    int getCallState(Context context);

    void setCallState(Context context, int callState);
}
