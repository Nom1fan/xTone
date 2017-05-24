package com.client;

import android.content.Context;

/**
 * Created by Mor on 24/05/2017.
 */

public abstract class BaseClient implements Client {

    protected Context context;

    public BaseClient(Context context) {
        this.context = context;
    }
}
