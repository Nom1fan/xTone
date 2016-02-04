package LogObjects;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

/**
 * Manages all the server side logs
 * @author Mor
 *
 */
public abstract class LogsManager {

	private static Logger _serverLogger = null;
    private static HashMap<String, Logger> _loggers = new HashMap<>();
	private static final String LOG_DIR = Paths.get("").toAbsolutePath().toString() + "\\server_side\\logs\\";
	
	/**
	 * @return The global server side logger
	 */
	public static Logger get_serverLogger() {
		
		if(_serverLogger ==null)
		{
			try 
			{
				_serverLogger = Logger.getLogger("Server");
				FileHandler fh;
				int limit = 1000000; // 1 Mb
				int numLogFiles = 3;
				String pattern = "Server"+"%g"+".log";
				fh = new FileHandler(LOG_DIR +pattern, limit ,numLogFiles, true);
				fh.setFormatter(new SingleLineFormatter());
				_serverLogger.addHandler(fh);
				_serverLogger.setUseParentHandlers(false);
			}
			catch (SecurityException | IOException e) {
				
				e.printStackTrace();
				System.out.println("ERROR: failed to get server logger");
			}		
		}
		
		return _serverLogger;
		
	}
		
	/**
	 * Returns a specific logger if exists, otherwise creates it and returns
	 * @param name - The logger name
	 * @return The logger
	 */
	public static Logger getLogger(String name) {
		
		Logger newLogger = _loggers.get(name);
        if(newLogger==null) {
            try {
                newLogger = Logger.getLogger(name);
                FileHandler fh;
                int limit = 1000000; // 1 Mb
                int numLogFiles = 3;
                String pattern = name + "%g" + ".log";
                fh = new FileHandler(LOG_DIR + pattern, limit, numLogFiles, true);
                fh.setFormatter(new SingleLineFormatter());
                newLogger.addHandler(fh);
                newLogger.setUseParentHandlers(false);

                _loggers.put(name, newLogger);
            } catch (SecurityException | IOException e) {

                e.printStackTrace();
            }
        }
		
		return newLogger;
	}

	/**
     * Returns a specific logger if exists, otherwise creates it in requested path and returns
     * @param name - The logger name
	 * @param logPath Specific path the logger will write to
     * @return The logger
	 */
	public static Logger getLogger(String name, String logPath) {

        Logger newLogger = _loggers.get(name);
        if(newLogger==null) {
            try {
                newLogger = Logger.getLogger(name);
                FileHandler fh;
                int limit = 1000000; // 1 Mb
                int numLogFiles = 3;
                String pattern = name + "%g" + ".log";
                fh = new FileHandler(logPath + pattern, limit, numLogFiles, true);
                fh.setFormatter(new SingleLineFormatter());
                newLogger.addHandler(fh);
                newLogger.setUseParentHandlers(false);

                _loggers.put(name, newLogger);
            } catch (SecurityException | IOException e) {

                e.printStackTrace();
            }
        }
        return newLogger;
	}
	
	/**
	 * Clears the log directory	
	 */
	public static void clearLogs() {

		try {
			FileUtils.cleanDirectory(new File(LOG_DIR));
		}
		catch(IOException e) {
			System.out.println("Did not clean logs dir:"+e.getMessage());
		}
	}
	
	/**
	 * Clears the log directory	
	 */
	public static void clearLogs(String logPath) {

		try {
			FileUtils.cleanDirectory(new File(logPath));
		}
		catch(IOException e) {
			System.out.println("Did not clean logs dir:"+e.getMessage());
		}
	}

    /**
     * Create the log directory
     */
	public static void createServerLogsDir() throws IOException {

        FileUtils.forceMkdir(new File(LOG_DIR));
	}
}
