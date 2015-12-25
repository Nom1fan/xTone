package com.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import DalObjects.IDAL;
import DataObjects.SharedConstants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by Mor on 19/12/2015.
 */
public class MySqlDAL implements IDAL {

    // Table users
    public static final String TABLE_USERS = "users";
    // Table keys under TABLE_USERS
    public static final String COL_UID = "uid";
    public static final String COL_TOKEN = "token";

    //Table communication_history
    public static final String TABLE_COMM_HISTORY = "communication_history";
    // Table keys under TABLE_COMM_HISTORY
    public static final String COL_UID_SRC = "uid_src";
    public static final String COL_UID_DEST = "uid_dest";
    public static final String COL_CONTENT_EXTENSION = "content_ext";
    public static final String COL_CONTENT_SIZE = "content_size";

    private Connection conn;

    public MySqlDAL() throws SQLException {

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

    @Override
    public boolean updateUserPushToken(String uid, String token) {

        String query = "UPDATE " + TABLE_USERS + " SET " + COL_TOKEN + "=" + "\"" + token + "\"" + " WHERE "+ COL_UID + "=" + "\"" +  uid + "\"";
        return executeQuery(query);
    }

    @Override
    public int insertCommunicationHistory(String src, String dest, String extension, int size) throws SQLException {



        String query = "INSERT INTO " + TABLE_COMM_HISTORY +
                " (" + COL_UID_SRC + "," + COL_UID_DEST + "," + COL_CONTENT_EXTENSION + "," + COL_CONTENT_SIZE + ")" +
                " VALUES" + " (" + "\"" + src + "\"" + "," + "\"" + dest + "\"" + "," + "\"" +  extension + "\"" + "," + "\"" + size + "\"" + ")";

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
