package com.server.database;

/**
 * Created by Mor on 25/07/2016.
 */
public interface SmsVerificationAccess {

    int NO_SMS_CODE = -1;

    boolean insertSmsVerificationCode(String uid, int code);
    int getSmsVerificationCode(String uid);
}
