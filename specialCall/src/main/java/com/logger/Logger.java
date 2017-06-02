package com.logger;

/**
 * Created by Mor on 31/05/2017.
 */

public interface Logger {

    void info(String TAG, String msg);

    void warn(String TAG, String msg);

    void debug(String TAG, String msg);

    void error(String TAG, String msg);

}
