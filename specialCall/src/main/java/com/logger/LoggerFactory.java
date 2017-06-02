package com.logger;

/**
 * Created by Mor on 31/05/2017.
 */

public abstract class LoggerFactory {

    public static Logger getLogger() {
        return new CrashlyticsLogger();
    }
}
