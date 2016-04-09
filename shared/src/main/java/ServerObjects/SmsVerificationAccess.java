package ServerObjects;

import java.sql.SQLException;

import DalObjects.DALAccesible;
import DalObjects.IDAL;

/**
 * Created by Mor on 28/03/2016.
 */
public class SmsVerificationAccess  extends DALAccesible {

    public static final int NO_SMS_CODE = -1;

    private SmsVerificationAccess(IDAL dal) {
        super(dal);
    }

    public static SmsVerificationAccess instance(IDAL idal) {

        return new SmsVerificationAccess(idal);
    }

    public boolean insertSmsVerificationCode(String uid, int code) {

        try {

            if(getSmsVerificationCode(uid)==SmsVerificationAccess.NO_SMS_CODE)
                _dal.insertUserSmsVerificationCode(uid, code);
            else
                _dal.updateUserSmsVerificationCode(uid, code);

            _logger.info("SMS verification code inserted for [User]:" + uid + ". [Code]:" + code);
            return true;
        } catch (SQLException e) {

            e.printStackTrace();
            _logger.severe("Failed to insert SMS verification code for [User]:" + uid + ". [Exception]:" + (e.getMessage()!=null ? e.getMessage(): e));
            return false;
        }
    }

    public int getSmsVerificationCode(String uid) {

        try {
            int code = _dal.getUserSmsVerificationCode(uid);
            return code!=0 ? code : NO_SMS_CODE;
        } catch (SQLException e) {
            e.printStackTrace();
            _logger.severe("Failed to get SMS verification code for [User]:" + uid + ". [Exception]:" + (e.getMessage()!=null ? e.getMessage(): e));
            return NO_SMS_CODE;
        }
    }
}
