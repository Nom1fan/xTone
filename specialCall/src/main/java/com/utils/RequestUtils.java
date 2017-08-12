package com.utils;

import android.content.Context;


import com.model.request.Request;

/**
 * Created by Mor on 12/08/2017.
 */

public interface RequestUtils extends Utility {
    void prepareDefaultRequest(Context context, Request request);

    Request getDefaultRequest(Context context);
}
