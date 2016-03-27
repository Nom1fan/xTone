package ServerObjects;

import DataObjects.UserStatus;
import DalObjects.DALAccesible;
import DalObjects.IDAL;



/**
 * This class is a singleton that manages all client connections
 *
 * @author Mor
 */
public class ClientsManager extends DALAccesible {

    public ClientsManager(IDAL dal) {
        super(dal);
    }

    private static void logActiveConns() {

////		Set<String> keys = onlineConnections.keySet();
////		String str = "Connected clients:"+keys.toString();
//
//		_logger.info(str);
    }

    public boolean registerUser(String userId, String token) {

        try {
            if (isRegistered(userId))
                _dal.updateUserPushToken(userId, token);
            else
                _dal.registerUser(userId, token);

            _logger.info("Registered [User]:" + userId + " successfully");
            return true;
        } catch (Exception e) {
            _logger.severe("RegisterUser failure. " + " [User]:" + userId +
                    "[Exception]:" + (e.getMessage() != null ? e.getMessage() : e));
            return false;
        }
    }

    public boolean unregisterUser(String userId, String token) {

        try {
            _dal.unregisterUser(userId, token);

            _logger.info("Unregistered [User]:" + userId + ". [Token]:" + token + " successfully");
            return true;
        } catch (Exception e) {
            _logger.severe("Unregistered failure. " + " [User]:" + userId +
                    "[Exception]:" + (e.getMessage() != null ? e.getMessage() : e));
            return false;
        }
    }

    public String getUserPushToken(String userId) {

        try {
            return _dal.getUserPushToken(userId);
        } catch (Exception e) {
            _logger.severe("getUserPushToken failure. " + " [User]:" + userId +
                    "[Exception]:" + (e.getMessage() != null ? e.getMessage() : e));
            return null;
        }
    }

    public boolean isRegistered(String clientId) {

        String deviceToken = getUserPushToken(clientId);

        if (deviceToken == null || deviceToken.equals("")) {

            _logger.info("No device token found for user:" + clientId + ". User is " + UserStatus.UNREGISTERED.toString());
            return false;
        }

        _logger.info("[User]:" + clientId + " is " + UserStatus.REGISTERED.toString());
        return true;
    }

}
