package com.handlers;

import com.google.gson.Gson;

/**
 * Created by Mor on 12/31/2016.
 */
public abstract class AbstractPushHandler implements PushHandler {

    protected Gson gson = new Gson();
}
