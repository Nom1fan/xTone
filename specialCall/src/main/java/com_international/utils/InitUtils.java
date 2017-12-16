package com_international.utils;

import android.content.Context;

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
}
