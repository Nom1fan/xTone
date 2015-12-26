package ServerObjects;


import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import ClientObjects.UserStatus;
import DalObjects.IDAL;
import DataObjects.SharedConstants;
import EventObjects.EventReport;
import LogObjects.LogsManager;
import MessagesToClient.MessageToClient;
import MessagesToClient.MessageTriggerEventOnly;


/**
 * This class is a singleton that manages all client connections
 * @author Mor
 */
public class ClientsManager {

	private static ClientsManager instance;

	private static Logger serverLogger = null;

	private static IDAL dal;

	private ClientsManager(IDAL dal) {
		
		initLoggers();
		this.dal = dal;
	}

	public static void initialize(IDAL dal) {

		if(instance==null)
			instance = new ClientsManager(dal);
	}
	
	private static void logActiveConns() { 
			
////		Set<String> keys = onlineConnections.keySet();
////		String str = "Connected clients:"+keys.toString();
//
//		serverLogger.info(str);
	}
	
	private void initLoggers() {
		
		serverLogger = LogsManager.getServerLogger();
	}

	public synchronized static boolean registerUser(String clientId, String token) {

		if(isRegistered(clientId))
			return dal.updateUserPushToken(clientId, token);
		else
			return dal.registerUser(clientId, token);
	}

	public synchronized static boolean unregisterUser(String clientId) {

		return dal.unregisterUser(clientId);
	}

	public synchronized static String getClientPushToken(String clientId) {

		return dal.getUserPushToken(clientId);
	}

	public synchronized static boolean isRegistered(String clientId) {

        String deviceToken = ClientsManager.getClientPushToken(clientId);

        if (deviceToken == null || deviceToken.equals("")) {

            serverLogger.info("No device token found for user:" + clientId + ". User is " + UserStatus.UNREGISTERED.toString());
            return false;
        }

		return true;
		
	}

}
