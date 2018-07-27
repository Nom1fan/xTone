package com.xtone.utils;

import android.content.Context;

import com.xtone.model.MediaFile;

public interface Phone2MediaUtils extends Utility {

    MediaFile getMediaFile(Context context, String phoneNumber);

    void setMediaFile(Context context, String phoneNumber, MediaFile mediaFile);
}
