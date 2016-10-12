package com.server.database.rowmappers;


import com.server.database.dbos.AppMetaDBO;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.server.database.Dao.COL_LAST_SUPPORTED_VER;


/**
 * Created by Mor on 27/09/2016.
 */
public class AppMetaRowMapper implements RowMapper<AppMetaDBO> {
    @Override
    public AppMetaDBO mapRow(ResultSet resultSet, int i) throws SQLException {
        return new AppMetaDBO(resultSet.getDouble(COL_LAST_SUPPORTED_VER));
    }
}
