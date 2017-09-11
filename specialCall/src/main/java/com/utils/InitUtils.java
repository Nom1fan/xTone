package com.utils;

import android.content.Context;

import com.bumptech.glide.util.Util;

/**
 * Created by Mor on 02/06/2017.
 */

public interface InitUtils extends Utility {
    void hideMediaFromGalleryScanner();

    void initializeSettingsDefaultValues(Context context);

    void populateSavedMcFromDiskToSharedPrefs(Context context);

    void saveAndroidVersion(Context context);

    void initImageLoader(Context context);

    void initSyncDefaultMediaReceiver(Context context);

    void syncContacts(Context context);
}
