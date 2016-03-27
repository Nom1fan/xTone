package ServerObjects;

import java.sql.SQLException;

import DalObjects.DALAccesible;
import DalObjects.IDAL;
import DataObjects.AppMetaRecord;
import log.Logged;

/**
 * Created by Mor on 26/03/2016.
 */
public class AppMetaManager extends DALAccesible {

    public AppMetaManager(IDAL dal) {
        super(dal);
    }

    public AppMetaRecord getAppMeta() {

        try {
            return _dal.getAppMetaRecord();
        } catch (SQLException e) {
            e.printStackTrace();
            _logger.severe("Failed to retrieve AppMetaRecord. [Exception]:" + (e.getMessage()!=null ? e.getMessage() : e));
        }

        return null;
    }
}
