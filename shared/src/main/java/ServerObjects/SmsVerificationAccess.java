package ServerObjects;

import java.sql.SQLException;

import DalObjects.DALAccesible;
import DalObjects.IDAL;
import utils.RandUtils;

/**
 * Created by Mor on 28/03/2016.
 */
public class SmsVerificationAccess  extends DALAccesible {

    private SmsVerificationAccess(IDAL dal) {
        super(dal);
    }

    public static SmsVerificationAccess instance(IDAL idal) {

        return new SmsVerificationAccess(idal);
    }

    public boolean insertSmsVerificationCode(String uid, int code) {

        try {

            if(getSmsVerificationCode(uid)==0)
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
            return _dal.getUserSmsVerificationCode(uid);
        } catch (SQLException e) {
            e.printStackTrace();
            _logger.severe("Failed to get SMS verification code for [User]:" + uid + ". [Exception]:" + (e.getMessage()!=null ? e.getMessage(): e));
            return -1;
        }
    }
}
