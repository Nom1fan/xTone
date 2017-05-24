package com.client;

import android.content.Context;

/**
 * Created by Mor on 24/05/2017.
 */

public abstract class ClientFactory {

    public static MediaClient getMediaClient(Context context) {
        return new MediaClientImpl(context);
    }
}
