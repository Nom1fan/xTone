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
    private String _deviceModel;
    private String _androidVersion;

    public UserRecord() {
    }

    public UserRecord(
            String uid,
            String token,
            Date registered_date,
            UserStatus userStatus,
            Date unregistered_date,
            int unregistered_count,
            String deviceModel,
            String androidVersion) {

        this._uid = uid;
        this._token = token;
        this._registered_date = registered_date;
        this._userStatus = userStatus;
        this._unregistered_date = unregistered_date;
        this._unregistered_count = unregistered_count;
        this._deviceModel = deviceModel;
        this._androidVersion = androidVersion;
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

    public String get_deviceModel() {
        return _deviceModel;
    }

    public String get_androidVersion() {
        return _androidVersion;
    }

    public void setAndroidVersion(String androidVersion) {

        this._androidVersion = androidVersion;
    }

    public void setUid(String uid) {

        this._uid = uid;
    }
}
