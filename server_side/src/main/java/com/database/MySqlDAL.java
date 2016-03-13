package com.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import DalObjects.IDAL;
import DataObjects.CallRecord;
import DataObjects.SharedConstants;
import DataObjects.TransferDetails;
import FilesManager.FileManager;

/**
 * Created by Mor on 19/12/2015.
 */
public class MySqlDAL implements IDAL {

    private Connection _dbConn;

    //region IDAL methods implementations
    @Override
    public void initConn() throws SQLException {

        _dbConn = DriverManager.getConnection("jdbc:mysql://" + SharedConstants.DB_SERVER_HOST + ":" + SharedConstants.DB_SERVER_PORT + "/sys?" +
                "user=" + SharedConstants.DB_SERVER_USER + "&password=" + SharedConstants.DB_SERVER_PWD);
    }

    @Override
    public void closeConn() {

        if (_dbConn != null)
            try {
                _dbConn.close();
            } catch (SQLException e) {
            } //ignore
    }

    @Override
    public void registerUser(String uid, String token) throws SQLException {

        StringBuilder query = new StringBuilder();
        uid = quote(uid);
        token = quote(token);

        query.
                append("INSERT INTO ").
                append(TABLE_USERS).
                append(" (").
                append(COL_UID).append(",").
                append(COL_TOKEN).
                append(")").
                append(" VALUES (").
                append(uid).append(",").
                append(token).
                append(")");

        executeQuery(query.toString());
    }

    @Override
    public void unregisterUser(String uid, String token) throws SQLException {

        StringBuilder query = new StringBuilder();
        uid = quote(uid);
        token = quote(token);

        query.
                append("DELETE FROM ").
                append(TABLE_USERS).
                append(" WHERE ").
                append(COL_UID).
                append("=").
                append(uid).
                append(" AND ").
                append(COL_TOKEN).
                append("=").
                append(token);

        executeQuery(query.toString());
    }

    @Override
    public String getUserPushToken(String uid) throws SQLException {

        String query = "SELECT " + COL_TOKEN + " FROM " + TABLE_USERS + " WHERE " + COL_UID + "=" + "\"" + uid + "\"";
        Statement st = null;
        ResultSet resultSet = null;
        String token = "";

        try {
            initConn(); // Must init before each query since after 8 hours the connection is timed out
            st = _dbConn.createStatement();
            resultSet = st.executeQuery(query);
            if (resultSet.first())
                token = resultSet.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (st != null)
                try {
                    st.close();
                } catch (SQLException e) {
                } // ignore
            if (resultSet != null)
                try {
                    resultSet.close();
                } catch (SQLException e) {
                } // ignore
            closeConn();
        }
        return token;
    }

    //TODO Mor: Test this method
    @Override
    public void updateMediaTransferRecord(int commId, String[] columns, Object[] values) throws SQLException {

        if (columns.length < 1)
            return;

        StringBuilder query = new StringBuilder();
        query.append("UPDATE " + TABLE_MEDIA_TRANSFERS + " SET " + columns[0] + "=" + "\"" + values[0] + "\"");
        for (int i = 1; i < columns.length; ++i) {
            query.append(", ");
            query.append(columns[i] + "=" + "\"" + values[i] + "\"");
        }

        query.append(" WHERE " + COL_TRANSFER_ID + "=" + "\"" + commId + "\"");
        executeQuery(query.toString());
    }

    @Override
    public void updateMediaTransferRecord(int commId, String column, Object value) throws SQLException {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sCommId = quote(String.valueOf(commId));
        String date = quote(sdf.format(new Date()));
        value = quote((String.valueOf(value)));

        StringBuilder query = new StringBuilder();

        query.
                append("UPDATE ").
                append(TABLE_MEDIA_TRANSFERS).
                append(" SET ").
                append(column).append("=").
                append(value).append(", ").
                append(COL_TRANSFER_DATETIME).append("=").
                append(date).
                append(" WHERE ").
                append(COL_TRANSFER_ID).append('=').
                append(sCommId);

        executeQuery(query.toString());
    }

    @Override
    public void updateUserPushToken(String uid, String token) throws SQLException {

        String query = "UPDATE " + TABLE_USERS + " SET " + COL_TOKEN + "=" + "\"" + token + "\"" + " WHERE " + COL_UID + "=" + "\"" + uid + "\"";
        executeQuery(query);
    }

    @Override
    public int insertMediaTransferRecord(TransferDetails td) throws SQLException {

        insertMediaFileRecord(td.getMd5(), td.getExtension(), (int)td.getFileSize(), COL_TRANSFER_COUNT);

        StringBuilder query = new StringBuilder();
        query.
                append("INSERT INTO ").
                append(TABLE_MEDIA_TRANSFERS).
                append(" (").
                append(COL_TYPE).append(",").
                append(COL_MD5).append(",").
                append(COL_UID_SRC).append(",").
                append(COL_UID_DEST).
                append(")").
                append(" VALUES (").
                append(quote(td.getSpMediaType().toString())).append(",").
                append(quote(td.getMd5())).append(",").
                append(quote(td.getSourceId())).append(",").
                append(quote(td.getDestinationId())).
                append(")");

        return executeReturnGenKeyQuery(query.toString());
    }

    @Override
    public void insertMediaCallRecord(CallRecord callRecord) throws SQLException {

        String type = callRecord.get_spMediaType().toString();
        FileManager visualMediaFile;
        FileManager audioMediaFile;
        String visualMd5 = null;
        String audioMd5 = null;

        if ((visualMediaFile = callRecord.get_visualMediaFile()) != null) {

            visualMd5 = callRecord.get_visualMd5();
            insertMediaFileRecord(visualMd5,
                    visualMediaFile.getFileExtension(),
                    (int) visualMediaFile.getFileSize(),
                    COL_CALL_COUNT);
        }

        if((audioMediaFile = callRecord.get_audioMediaFile()) != null) {

            audioMd5 = callRecord.get_audioMd5();
            insertMediaFileRecord(audioMd5,
                    audioMediaFile.getFileExtension(),
                    (int) audioMediaFile.getFileSize(),
                    COL_CALL_COUNT);
        }

        StringBuilder query = new StringBuilder();
        query.
                append("INSERT INTO ").
                append(TABLE_MEDIA_CALLS).
                append(" (").
                append(COL_TYPE).append(",").
                append(COL_MD5_VISUAL).append(",").
                append(COL_MD5_AUDIO).append(",").
                append(COL_UID_SRC).append(",").
                append(COL_UID_DEST).
                append(")").
                append(" VALUES (").
                append(quote(type)).append(",").
                append(visualMd5!=null ? quote(visualMd5) : null).append(",").
                append(audioMd5!=null ? quote(audioMd5) : null).append(",").
                append(quote(callRecord.get_sourceId())).append(",").
                append(quote(callRecord.get_destinationId())).
                append(")");

        executeQuery(query.toString());
    }

    @Override
    public void insertMediaFileRecord(String md5, String extension, int size, String countColToInc) throws SQLException {

        StringBuilder query = new StringBuilder();
        query.
                append("INSERT IGNORE INTO ").
                append(TABLE_MEDIA_FILES).
                append(" (").
                append(COL_MD5).append(",").
                append(COL_CONTENT_EXTENSION).append(",").
                append(COL_CONTENT_SIZE).
                append(")").
                append(" VALUES (").
                append(quote(md5)).append(",").
                append(quote(extension)).append(",").
                append(quote(String.valueOf(size))).
                append(")");

        executeQuery(query.toString());

        incrementColumn(TABLE_MEDIA_FILES, countColToInc, COL_MD5, md5);
    }

    @Override
    public void incrementColumn(String table, String col, String whereCol, String whereVal) throws SQLException {

        StringBuilder query = new StringBuilder();

        query.
                append("UPDATE ").
                append(table).
                append(" SET ").
                append(col).
                append("=").
                append(col).append(" + 1").
                append( " WHERE ").
                append(whereCol).
                append("=").
                append(quote(whereVal));

        executeQuery(query.toString());
    }
    //endregion

    //region Internal operations methods and helpers
    private int executeReturnGenKeyQuery(String query) throws SQLException {

        PreparedStatement preparedStatement = null;
        try {
            initConn(); // Must init before each query since after 8 hours the connection is timed out
            preparedStatement = _dbConn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.executeUpdate();
            ResultSet res = preparedStatement.getGeneratedKeys();
            if (res.next())
                return res.getInt(1);
            throw new SQLException("No generated key returned from query");

        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (preparedStatement != null)
                try {
                    preparedStatement.close();
                    preparedStatement = null;
                } catch (SQLException e) {
                } //ignore
            closeConn();
        }
    }

    private void executeQuery(String query) throws SQLException {


        Statement stmt = null;
        try {
            initConn(); // Must init before each query since after 8 hours the connection is timed out
            stmt = _dbConn.createStatement();
            stmt.execute(query);
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                    stmt = null;
                } catch (SQLException e) {
                } // ignore
            }
            closeConn();
        }
    }

    private String quote(String str) {
        return "\"" + str + "\"";
    }
    //endregion
}
