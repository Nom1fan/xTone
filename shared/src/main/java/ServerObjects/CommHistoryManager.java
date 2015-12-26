package ServerObjects;

import java.sql.SQLException;
import java.util.logging.Logger;

import DalObjects.IDAL;
import LogObjects.LogsManager;

/**
 * Created by Mor on 26/12/2015.
 */
public class CommHistoryManager {

    private static CommHistoryManager instance;

    private static Logger serverLogger = null;

    private static IDAL dal;

    private CommHistoryManager(IDAL dal) {

        initLoggers();
        this.dal = dal;
    }

    public static void initialize(IDAL dal) {

        if(instance==null)
            instance = new CommHistoryManager(dal);
    }

    private void initLoggers() {

        serverLogger = LogsManager.getServerLogger();
    }

    public synchronized static int insertCommunicationRecord(String src, String dest, String extension, int size)  {

        try
        {
            int commId = dal.insertCommunicationHistory(src, dest, extension, size);
            serverLogger.info("insertCommunicationHistory success: [commId:" + commId + ", src:" + src + ", extension:" + extension + ", size:" + size + "]");
            return commId;
        } catch (SQLException e)
        {
            e.printStackTrace();
            serverLogger.severe("insertCommunicationHistory failure. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
            return -1;
        }
    }

    public synchronized static void updateCommunicationRecord(int commId, String[] columns, Object[] values) {

        try {
            serverLogger.info("updateCommunicationRecord success: [commId:" + commId + ", columns:" + columns.toString() + ", values:" + values.toString() + "]");
            dal.updateCommunicationRecord(commId, columns, values);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            serverLogger.severe("updateCommunicationRecord failure. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }

    public synchronized static void updateCommunicationRecord(int commId, String column, Object value) {

        try {
            serverLogger.info("updateCommunicationRecord success: [commId:" + commId + ", column:" + column + ", value:" + value + "]");
            dal.updateCommunicationRecord(commId, column, value);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            serverLogger.severe("updateCommunicationRecord failure. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }
}
