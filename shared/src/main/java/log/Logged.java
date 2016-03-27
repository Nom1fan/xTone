package log;

import java.util.logging.Logger;

import LogObjects.LogsManager;

/**
 * Created by Mor on 26/03/2016.
 */
public abstract class Logged {

    protected Logger _logger;

    private void initLoggers() {

        _logger = LogsManager.get_serverLogger();
    }

    public Logged() { initLoggers(); }
}


