package ServerObjects;

import java.sql.SQLException;
import java.util.List;

import DataObjects.MediaTransferRecord;
import DataObjects.PushEventKeys;
import DataObjects.TransferDetails;
import DataObjects.UserRecord;
import DataObjects.UserStatus;
import DalObjects.DALAccesible;
import DalObjects.IDAL;



/**
 * This class is a singleton that manages all client connections
 *
 * @author Mor
 */
public class UsersDataAccess extends DALAccesible {

    private UsersDataAccess(IDAL dal) {
        super(dal);
    }

    public static UsersDataAccess instance(IDAL idal) {

        return new UsersDataAccess(idal);
    }

    public boolean registerUser(String userId, String token) {

        try {
            UserRecord record = getUserRecord(userId);

            // User registering for the first time
            if(record == null) {

                _dal.registerUser(userId, token);
            }
            else {
                // User re-registering
                _dal.reRegisterUser(userId, token);
            }

            _logger.info("Registered [User]:" + userId + " successfully");
            return true;
        } catch (Exception e) {
            _logger.severe("RegisterUser failure. " + " [User]:" + userId +
                    " [Exception]:" + (e.getMessage() != null ? e.getMessage() : e));
            return false;
        }
    }

    public boolean unregisterUser(String userId, String token) {

        try {
            List<MediaTransferRecord> records = CommHistoryAccess.instance(_dal).getAllUserMediaTransferRecords(userId);

            // Clearing all media sent to destinations by user
            for(MediaTransferRecord record : records) {

                if(record.is_transfer_success()) {
                    String pushEventAction = PushEventKeys.CLEAR_MEDIA;
                    TransferDetails td = new TransferDetails(userId, record.get_dest_uid(), record.get_specialMediaType());
                    String destToken = UsersDataAccess.instance(_dal).getUserPushToken(record.get_dest_uid());
                    boolean sent = BatchPushSender.sendPush(destToken, pushEventAction, td);
                    if (!sent)
                        throw new Exception("Failed to send push notification");
                }
            }

            _dal.unregisterUser(userId, token);

            _logger.info("Unregistered [User]:" + userId + ". [Token]:" + token + " successfully");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            _logger.severe("Unregistered failure. " + " [User]:" + userId +
                    "[Exception]:" + (e.getMessage() != null ? e.getMessage() : e));
            return false;
        }
    }

    public String getUserPushToken(String userId) {

        return getUserRecord(userId).get_token();
    }

    public UserRecord getUserRecord(String userId) {

        try {
            return _dal.getUserRecord(userId);
        } catch(SQLException e) {
            _logger.severe("getUserRecord failure. " + " [User]:" + userId +
                    "[Exception]:" + (e.getMessage() != null ? e.getMessage() : e));
            return null;
        }
    }

    public boolean isRegistered(String userId) {

        UserRecord record;
        try {

            record = _dal.getUserRecord(userId);

            if (record == null) {

                _logger.info("[User]:" + userId + " is " + UserStatus.UNREGISTERED.toString());
                return false;
            }

            if (record.get_userStatus().equals(UserStatus.REGISTERED)) {

                _logger.info("[User]:" + userId + " is " + UserStatus.REGISTERED.toString());
                return true;
            }

        } catch (Exception e) {
            _logger.severe("isRegistered failure. " + " [User]:" + userId +
                    "[Exception]:" + (e.getMessage() != null ? e.getMessage() : e));
            return false;
        }

        _logger.info("[User]:" + userId + " is " + record.get_userStatus());
        return false;
    }

}
