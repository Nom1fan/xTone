package com.server.database;

import DataObjects.UserDBO;

/**
 * Created by Mor on 25/07/2016.
 */
public interface UserDataAccess {
    boolean unregisterUser(String userId, String token);

    boolean isRegistered(String userId);

    UserDBO getUserRecord(String destId);
}