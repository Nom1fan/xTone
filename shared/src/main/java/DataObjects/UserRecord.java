package DataObjects;

import java.util.Date;

/**
 * Created by Mor on 01/04/2016.
 */
public class UserRecord {

    private String _uid;
    private String _token;
    private Date _registered_date;
    private UserStatus _userStatus;
    private Date _unregistered_date;
    private int _unregistered_count;

    public UserRecord(String _uid, String _token, Date _registered_date, UserStatus userStatus, Date _unregistered_date, int _unregistered_count) {
        this._uid = _uid;
        this._token = _token;
        this._registered_date = _registered_date;
        this._userStatus = userStatus;
        this._unregistered_date = _unregistered_date;
        this._unregistered_count = _unregistered_count;
    }

    public String get_uid() {
        return _uid;
    }

    public String get_token() {
        return _token;
    }

    public Date get_registered_date() {
        return _registered_date;
    }

    public UserStatus get_userStatus() {
        return _userStatus;
    }

    public Date get_unregistered_date() {
        return _unregistered_date;
    }

    public int get_unregistered_count() {
        return _unregistered_count;
    }
}
