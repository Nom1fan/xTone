package com.xtone.utils;

import android.content.Context;

import com.google.gson.Gson;
import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;
import com.xtone.model.MediaFile;

public class Phone2MediaUtilsImpl implements Phone2MediaUtils {

    private SharedPrefUtils sharedPrefUtils = UtilsFactory.instance().getUtility(SharedPrefUtils.class);

    private Logger log = LoggerFactory.getLogger();

    private Gson gson = new Gson();

    @Override
    public MediaFile getMediaFile(Context context, String phoneNumber) {
        String mediaFileJson = sharedPrefUtils.getString(context, "PHONE_2_MEDIA", phoneNumber);
        return gson.fromJson(mediaFileJson, MediaFile.class);
    }

    @Override
    public void setMediaFile(Context context, String phoneNumber, MediaFile mediaFile) {
        sharedPrefUtils.setString(context, "PHONE_2_MEDIA", phoneNumber, gson.toJson(mediaFile));
    }
}
