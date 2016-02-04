package ServerObjects;

import java.sql.SQLException;
import java.util.logging.Logger;

import DalObjects.IDAL;
import LogObjects.LogsManager;

/**
 * Created by Mor on 26/12/2015.
 */
public class CommHistoryManager {

    private  Logger _logger = null;

    private  IDAL _dal;

    public CommHistoryManager(IDAL dal) {

        initLoggers();
        this._dal = dal;
    }

    private void initLoggers() {

        _logger = LogsManager.get_serverLogger();
    }

    public int insertCommunicationRecord(String type, String src, String dest, String extension, int size) {

        try {
            int commId = _dal.insertCommunicationHistory(type, src, dest, extension, size);
            StringBuilder builder = new StringBuilder();
            builder.
                    append("insertCommunicationHistory success: ").
                    append("[commId]:").append(commId).
                    append(", [src]:").append(src).
                    append(", [dest]:").append(dest).
                    append(", [extension]:").append(extension).
                    append(", [size]:").append(size);

            _logger.info(builder.toString());
            return commId;
        } catch (SQLException e) {
            e.printStackTrace();
            _logger.severe("insertCommunicationHistory failure. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
            return -1;
        }
    }

    public void updateCommunicationRecord(int commId, String[] columns, Object[] values) {

        try {
            _logger.info("updateCommunicationRecord success: [commId:" + commId + ", columns:" + columns.toString() + ", values:" + values.toString() + "]");
            _dal.updateCommunicationRecord(commId, columns, values);
        } catch (SQLException e) {
            e.printStackTrace();
            _logger.severe("updateCommunicationRecord failure. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

    public void updateCommunicationRecord(int commId, String column, Object value) {

        try {
            _dal.updateCommunicationRecord(commId, column, value);
            _logger.info("updateCommunicationRecord success: [commId:" + commId + ", column:" + column + ", value:" + value + "]");
        } catch (SQLException e) {
            e.printStackTrace();
            _logger.severe("updateCommunicationRecord failure. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

}
