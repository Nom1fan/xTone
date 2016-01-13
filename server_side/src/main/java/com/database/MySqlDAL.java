package com.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import DalObjects.IDAL;
import DataObjects.SharedConstants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Mor on 19/12/2015.
 */
public class MySqlDAL implements IDAL {

    private Connection conn;

    public MySqlDAL() throws SQLException {

        if(conn==null)
            initConn();
    }

    public void initConn() throws SQLException {

        conn = DriverManager.getConnection("jdbc:mysql://" + SharedConstants.DB_SERVER_HOST + ":" + SharedConstants.DB_SERVER_PORT + "/sys?" +
                "user=" + SharedConstants.DB_SERVER_USER + "&password=" + SharedConstants.DB_SERVER_PWD);
    }

    @Override
    public boolean registerUser(String uid, String token) {

        String query = "INSERT INTO " + TABLE_USERS + " (" + COL_UID + "," + COL_TOKEN + ")" +
                " VALUES (" + "\"" + uid + "\"" + "," + "\"" + token + "\"" + ")";
        return executeQuery(query);
    }

    @Override
    public boolean unregisterUser(String uid) {

        String query = "DELETE FROM " + TABLE_USERS + " WHERE " + COL_UID + "=" + "\"" + uid + "\"";
        return executeQuery(query);
    }

    @Override
    public String getUserPushToken(String uid) {

        String query = "SELECT " + COL_TOKEN + " FROM " + TABLE_USERS + " WHERE " + COL_UID + "=" + "\"" + uid + "\"";
        Statement st = null;
        ResultSet resultSet = null;
        String token = "";
        try {
            initConn(); // Must init before each query since after 8 hours the connection is timed out
            st = conn.createStatement();
            resultSet = st.executeQuery(query);
            if(resultSet.first())
                token = resultSet.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            if (st != null)
                try {
                    st.close();
                } catch (SQLException e) { } // ignore
            if(resultSet != null)
                try {
                    resultSet.close();
                } catch (SQLException e) { } // ignore
        }
        return token;
    }

    //TODO Test this method
    @Override
    public void updateCommunicationRecord(int commId, String[] columns, Object[] values) throws SQLException {

        if(columns.length < 1)
            return;

        StringBuilder query = new StringBuilder();
        query.append("UPDATE " + TABLE_COMM_HISTORY + " SET " + columns[0] + "=" + "\"" + values[0] + "\"");
        for(int i=1;i<columns.length;++i) {
            query.append(", ");
            query.append(columns[i] + "=" + "\"" + values[i] + "\"");
        }

        query.append(" WHERE " + COL_COMM_ID + "=" + "\"" + commId + "\"");
        executeQuery(query.toString());
    }

    @Override
    public void updateCommunicationRecord(int commId, String column, Object value) throws SQLException{

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String query =
                "UPDATE " + TABLE_COMM_HISTORY +
                " SET " + column + "=" + "\"" + value + "\"" +
                ", " + COL_TRANSFER_DATETIME + "=" + "\"" + sdf.format(new Date()) + "\"" +
                " WHERE " + COL_COMM_ID + "=" + "\"" + commId + "\"";
        boolean isOK = executeQuery(query);
        if(!isOK)
            throw new SQLException("updateCommunicationRecord failed. Check stack trace for more information.");
    }

    @Override
    public boolean updateUserPushToken(String uid, String token) {

        String query = "UPDATE " + TABLE_USERS + " SET " + COL_TOKEN + "=" + "\"" + token + "\"" + " WHERE "+ COL_UID + "=" + "\"" +  uid + "\"";
        return executeQuery(query);
    }

    @Override
    public int insertCommunicationHistory(String type, String src, String dest, String extension, int size) throws SQLException {

        String query = "INSERT INTO " + TABLE_COMM_HISTORY +
                " (" +
                COL_TYPE + "," +
                COL_UID_SRC + "," +
                COL_UID_DEST + "," +
                COL_CONTENT_EXTENSION + "," +
                COL_CONTENT_SIZE +
                ")" +
                " VALUES" +
                " (" +
                "\"" + type + "\"" + "," +
                "\"" + src + "\"" + "," +
                "\"" + dest + "\"" + "," +
                "\"" + extension + "\"" + "," +
                "\"" + size + "\"" +
                ")";

        return executeReturnGenKeyQuery(query);
    }

    private int executeReturnGenKeyQuery(String query) throws SQLException {

        PreparedStatement preparedStatement = null;
        try {
            initConn(); // Must init before each query since after 8 hours the connection is timed out
            preparedStatement = conn.prepareStatement(query , Statement.RETURN_GENERATED_KEYS);
            preparedStatement.executeUpdate();
            ResultSet res = preparedStatement.getGeneratedKeys();
            if(res.next())
                return res.getInt(1);
           throw new SQLException("No generated key returned from query");

        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        finally {
            if(preparedStatement!=null)
                try{
                    preparedStatement.close();
                } catch(SQLException e) { } //ignore
        }
    }

    private boolean executeQuery(String query) {

        boolean isOK = false;
        Statement stmt = null;
        try {
            initConn(); // Must init before each query since after 8 hours the connection is timed out
            stmt = conn.createStatement();
            stmt.execute(query);
            isOK = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            if(stmt!=null) {
                try {
                    stmt.close();
                } catch (SQLException e) { } // ignore
            }
            stmt = null;
        }

        return isOK;
    }
}
