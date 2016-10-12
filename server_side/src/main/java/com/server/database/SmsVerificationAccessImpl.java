package com.server.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Created by Mor on 28/03/2016.
 */
@Component
public class SmsVerificationAccessImpl implements SmsVerificationAccess {

    @Autowired
    private Dao dao;
    
    @Autowired
    private Logger logger;

    @Override
    public boolean insertSmsVerificationCode(String uid, int code) {

        try {

            if(getSmsVerificationCode(uid)== SmsVerificationAccess.NO_SMS_CODE)
                dao.insertUserSmsVerificationCode(uid, code);
            else
                dao.updateUserSmsVerificationCode(uid, code);

            logger.info("SMS verification code inserted for [User]:" + uid + ". [Code]:" + code);
            return true;
        } catch (SQLException e) {

            e.printStackTrace();
            logger.severe("Failed to insert SMS verification code for [User]:" + uid + ". [Exception]:" + (e.getMessage()!=null ? e.getMessage(): e));
            return false;
        }
    }

    @Override
    public int getSmsVerificationCode(String uid) {

        try {
            int code = dao.getSmsVerificationCode(uid);
            return code!=0 ? code : NO_SMS_CODE;
        } catch (SQLException e) {
            e.printStackTrace();
            logger.severe("Failed to get SMS verification code for [User]:" + uid + ". [Exception]:" + (e.getMessage()!=null ? e.getMessage(): e));
            return NO_SMS_CODE;
        }
    }
}
