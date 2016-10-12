package com.server.database.rowmappers;

import com.server.database.dbos.SmsVerificationDBO;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.server.database.Dao.COL_CODE;
import static com.server.database.Dao.COL_UID;


/**
 * Created by Mor on 27/09/2016.
 */
public class SmsVerificationRowMapper implements RowMapper<SmsVerificationDBO> {
    @Override
    public SmsVerificationDBO mapRow(ResultSet resultSet, int i) throws SQLException {
        return new SmsVerificationDBO(resultSet.getString(COL_UID), resultSet.getInt(COL_CODE));
    }
}
