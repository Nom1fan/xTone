package com.server.database.rowmappers;

import com.server.database.dbos.MediaFileDBO;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.server.database.Dao.COL_CONTENT_EXTENSION;
import static com.server.database.Dao.COL_CONTENT_SIZE;
import static com.server.database.Dao.COL_MD5;


/**
 * Created by Mor on 27/09/2016.
 */
public class MediaFileRowMapper implements RowMapper<MediaFileDBO> {
    @Override
    public MediaFileDBO mapRow(ResultSet resultSet, int i) throws SQLException {
        return new MediaFileDBO(resultSet.getString(COL_MD5), resultSet.getString(COL_CONTENT_EXTENSION), resultSet.getLong(COL_CONTENT_SIZE));
    }
}
