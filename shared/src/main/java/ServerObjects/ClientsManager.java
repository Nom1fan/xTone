package ServerObjects;


import java.util.logging.Logger;
import ClientObjects.UserStatus;
import DalObjects.IDAL;
import LogObjects.LogsManager;


/**
 * This class is a singleton that manages all client connections
 * @author Mor
 */
public class ClientsManager {

	private Logger _logger = null;

	private  IDAL _dal;

	public ClientsManager(IDAL dal) {
		
		initLoggers();
		this._dal = dal;
	}

	private static void logActiveConns() { 
			
////		Set<String> keys = onlineConnections.keySet();
////		String str = "Connected clients:"+keys.toString();
//
//		_logger.info(str);
	}
	
	private void initLoggers() {
		
		_logger = LogsManager.get_serverLogger();
	}

	public boolean registerUser(String clientId, String token) {

		if(isRegistered(clientId))
			return _dal.updateUserPushToken(clientId, token);
		else
			return _dal.registerUser(clientId, token);
	}

	public boolean unregisterUser(String clientId, String token) {

		_logger.info("Unregistering [User]:" + clientId + ". [Token]:" + token);
		return _dal.unregisterUser(clientId, token);
	}

	public String getClientPushToken(String clientId) {

		return _dal.getUserPushToken(clientId);
	}

	public boolean isRegistered(String clientId) {

        String deviceToken = getClientPushToken(clientId);

        if (deviceToken == null || deviceToken.equals("")) {

            _logger.info("No device token found for user:" + clientId + ". User is " + UserStatus.UNREGISTERED.toString());
            return false;
        }

        _logger.info("[User]:" + clientId + " is " + UserStatus.REGISTERED.toString());
		return true;
	}

}
