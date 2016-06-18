package com.database;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import DataObjects.CallRecord;
import DataObjects.MediaTransferRecord;

/**
 * Created by Mor on 26/12/2015.
 */
public class CommHistoryAccess extends DALAccesible {

    private CommHistoryAccess(IDAO dal) {
        super(dal);
    }

    public static CommHistoryAccess instance(IDAO IDAO) {

        return new CommHistoryAccess(IDAO);
    }

    public int insertMediaTransferRecord(Map data) {

        try {

            int commId = _dal.insertMediaTransferRecord(data);

            _logger.info("insertMediaTransferRecord success: " + data);
            return commId;
        } catch (SQLException e) {
            e.printStackTrace();
            _logger.severe("insertMediaTransferRecord failure. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
            return -1;
        }
        catch(Exception e) {
            e.printStackTrace();
            _logger.severe("insertMediaTransferRecord failure. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
            return -1;
        }
    }

    public void insertMediaCallRecord(CallRecord callRecord) {

        try {
            _dal.insertMediaCallRecord(callRecord);

            _logger.info("insertMediaCallRecord success: " + callRecord.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            _logger.severe("insertMediaCallRecord failure. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

    public void updateCommunicationRecord(int commId, String[] columns, Object[] values) {

        try {
            _logger.info("updateMediaTransferRecord success: [commId]:" + commId + ", [columns]:" + columns.toString() + ", [values]:" + values.toString());
            _dal.updateMediaTransferRecord(commId, columns, values);
        } catch (SQLException e) {
            e.printStackTrace();
            _logger.severe("updateMediaTransferRecord failure. [Exception]:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

    public void updateCommunicationRecord(int commId, String column, Object value) {

        try {
            _dal.updateMediaTransferRecord(commId, column, value);
            _logger.info("updateMediaTransferRecord success: [commId]:" + commId + ", [column]:" + column + ", [value]:" + value);
        } catch (SQLException e) {
            e.printStackTrace();
            _logger.severe("updateMediaTransferRecord failure. [Exception]:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

    public List<MediaTransferRecord> getAllUserMediaTransferRecords(String uid) {

        try {
            return _dal.getAllUserMediaTransferRecords(uid);
        } catch (SQLException e) {
            e.printStackTrace();
            _logger.severe("Failed to get all media transfers records for [User]:" + uid);
            return null;
        }
    }

}
