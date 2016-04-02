package DataObjects;

import java.util.Date;

/**
 * Created by Mor on 31/03/2016.
 */
public class MediaTransferRecord {

    private int _transfer_id;
    private SpecialMediaType _specialMediaType;
    private String _md5;
    private String _src_uid;
    private String _dest_uid;
    private Date _dateTime;
    private boolean _transfer_success;
    private Date _transfer_datetime;

    public MediaTransferRecord(int _transfer_id,
                               SpecialMediaType _specialMediaType,
                               String _md5,
                               String _src_uid,
                               String _dest_uid,
                               Date _dateTime,
                               boolean _transfer_success,
                               Date _transfer_datetime) {

        this._transfer_id = _transfer_id;
        this._specialMediaType = _specialMediaType;
        this._md5 = _md5;
        this._src_uid = _src_uid;
        this._dest_uid = _dest_uid;
        this._dateTime = _dateTime;
        this._transfer_success = _transfer_success;
        this._transfer_datetime = _transfer_datetime;
    }

    public int get_transfer_id() {
        return _transfer_id;
    }

    public SpecialMediaType get_specialMediaType() {
        return _specialMediaType;
    }

    public String get_md5() {
        return _md5;
    }

    public String get_src_uid() {
        return _src_uid;
    }

    public String get_dest_uid() {
        return _dest_uid;
    }

    public Date get_dateTime() {
        return _dateTime;
    }

    public boolean is_transfer_success() {
        return _transfer_success;
    }

    public Date get_transfer_datetime() {
        return _transfer_datetime;
    }
}
