package com.server.database.rowmappers;


import com.server.database.dbos.UserDBO;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import DataObjects.UserStatus;

import static com.server.database.Dao.COL_ANDROID_VERSION;
import static com.server.database.Dao.COL_DEVICE_MODEL;
import static com.server.database.Dao.COL_REGISTERED_DATE;
import static com.server.database.Dao.COL_TOKEN;
import static com.server.database.Dao.COL_UID;
import static com.server.database.Dao.COL_UNREGISTERED_COUNT;
import static com.server.database.Dao.COL_UNREGISTERED_DATE;
import static com.server.database.Dao.COL_USER_STATUS;


/**
 * Created by Mor on 27/09/2016.
 */
public class UserDboRowMapper implements RowMapper<UserDBO> {
    @Override
    public UserDBO mapRow(ResultSet resultSet, int i) throws SQLException {
        return new UserDBO(resultSet.getString(COL_UID),
                            resultSet.getString(COL_TOKEN),
                            resultSet.getDate(COL_REGISTERED_DATE),
                            UserStatus.valueOf(resultSet.getString(COL_USER_STATUS)),
                            resultSet.getDate(COL_UNREGISTERED_DATE),
                            resultSet.getInt(COL_UNREGISTERED_COUNT),
                            resultSet.getString(COL_DEVICE_MODEL),
                            resultSet.getString(COL_ANDROID_VERSION));
    }
}
