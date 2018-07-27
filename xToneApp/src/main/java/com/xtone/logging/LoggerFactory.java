package com.xtone.logging;

/**
 * Created by Mor on 31/05/2017.
 */

public abstract class LoggerFactory {

    private static Logger logger = new CrashlyticsLogger();

    public static void setLogger(Logger newLogger) {
        logger = newLogger;
    }

    public static Logger getLogger() {
        return logger;
    }
}
