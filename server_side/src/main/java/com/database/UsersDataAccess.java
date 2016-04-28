package com.database;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import DataObjects.DataKeys;
import DataObjects.MediaTransferRecord;
import DataObjects.PushEventKeys;
import DataObjects.SpecialMediaType;
import DataObjects.UserRecord;
import DataObjects.UserStatus;
import pushservice.BatchPushSender;


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

            Set<String> destinations = new HashSet<>();

            // Creating a set of all destinations who received media from the user
            for(MediaTransferRecord record : records) {
                if(record.is_transfer_success())
                    destinations.add(record.get_dest_uid());
            }

            // Clearing all media sent to these destinations by user
            for(String destination : destinations) {

                String pushEventAction = PushEventKeys.CLEAR_MEDIA;
                String destToken = UsersDataAccess.instance(_dal).getUserPushToken(destination);

                final SpecialMediaType[] specialMediaTypes = SpecialMediaType.values();

                // Clearing all types of special media
                for(SpecialMediaType specialMediaType : specialMediaTypes) {

                    HashMap<DataKeys,Object> data = new HashMap();
                    data.put(DataKeys.SPECIAL_MEDIA_TYPE, specialMediaType);
                    data.put(DataKeys.SOURCE_ID, userId);
                    boolean sent = BatchPushSender.sendPush(destToken, pushEventAction, data);
                    if (!sent)
                        _logger.warning("Failed to send push to clear media. [User]:" + destination + " [SpecialMediaType]:" + specialMediaType);
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
