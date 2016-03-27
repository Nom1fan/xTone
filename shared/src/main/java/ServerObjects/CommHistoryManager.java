package ServerObjects;

import java.sql.SQLException;
import java.util.logging.Logger;

import DalObjects.DALAccesible;
import DalObjects.IDAL;
import DataObjects.CallRecord;
import DataObjects.SpecialMediaType;
import DataObjects.TransferDetails;
import FilesManager.FileManager;
import LogObjects.LogsManager;

/**
 * Created by Mor on 26/12/2015.
 */
public class CommHistoryManager extends DALAccesible {

    public CommHistoryManager(IDAL dal) {
        super(dal);
    }

    public int insertMediaTransferRecord(TransferDetails td) {

        try {

            int commId = _dal.insertMediaTransferRecord(td);

            _logger.info("insertMediaTransferRecord success: " + td);
            return commId;
        } catch (SQLException e) {
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

}
