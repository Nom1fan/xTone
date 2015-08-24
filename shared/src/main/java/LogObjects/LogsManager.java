package LogObjects;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

/**
 * Manages all the server side logs
 * @author Mor
 *
 */
public abstract class LogsManager {

	private static Logger serverLogger = null;
	
	/**
	 * Returns the global server side logger
	 * @param name - The name in which the server side logger will be called
	 * @return The global server side logger
	 */
	public static Logger getServerLogger(String name) {
		
		if(serverLogger==null)
		{
			try 
			{
				serverLogger = Logger.getLogger(name);			
				Path currentRelativePath = Paths.get("");
				String log_dir = currentRelativePath.toAbsolutePath().toString() + "\\logs\\";
				FileHandler fh = null;
				int limit = 1000000; // 1 Mb
				int numLogFiles = 3;
				String pattern = name+"%g"+".log";
				fh = new FileHandler(log_dir+pattern, limit ,numLogFiles, true);									
				fh.setFormatter(new SingleLineFormatter());
				serverLogger.addHandler(fh);
				serverLogger.setUseParentHandlers(false);
			}
			catch (SecurityException | IOException e) {
				
				e.printStackTrace();
			}		
		}
		
		return serverLogger;
		
	}
		
	/**
	 * Returns a new, specific logger
	 * @param name - The name in which the new logger will be called
	 * @return The new logger
	 */
	public static Logger getNewLogger(String name) {
		
		Logger newLogger = null;
		try 
		{
			newLogger = Logger.getLogger(name);			
			Path currentRelativePath = Paths.get("");
			String log_dir = currentRelativePath.toAbsolutePath().toString() + "\\logs\\";
			FileHandler fh = null;
			int limit = 1000000; // 1 Mb
			int numLogFiles = 3;
			String pattern = name+"%g"+".log";
			fh = new FileHandler(log_dir+pattern, limit ,numLogFiles, true);									
			fh.setFormatter(new SingleLineFormatter());
			newLogger.addHandler(fh);
			newLogger.setUseParentHandlers(false);
		}
		catch (SecurityException | IOException e) {
			
			e.printStackTrace();
		}		
		
		return newLogger;
	}
	
	/**
	 * Clears the log directory	
	 */
	public static void clearLogs() {
		
		Path currentRelativePath = Paths.get("");
		String log_dir = currentRelativePath.toAbsolutePath().toString() + "\\logs\\"; 
		File logFolder = new File(log_dir);
		
		try {
			FileUtils.cleanDirectory(logFolder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}
