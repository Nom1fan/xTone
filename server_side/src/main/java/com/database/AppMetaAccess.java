package com.database;

import java.sql.SQLException;

import com.database.records.AppMetaRecord;

/**
 * Created by Mor on 26/03/2016.
 */
public class AppMetaAccess extends DALAccesible {

    private AppMetaAccess(IDAL dal) {
        super(dal);
    }

    public static AppMetaAccess instance(IDAL idal) {

        return new AppMetaAccess(idal);
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
