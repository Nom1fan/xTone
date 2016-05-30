package com.database;

import com.database.records.AppMetaRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import DataObjects.CallRecord;
import DataObjects.DataKeys;
import DataObjects.MediaTransferRecord;
import DataObjects.SharedConstants;
import DataObjects.SpecialMediaType;
import DataObjects.UserRecord;
import DataObjects.UserStatus;
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
            } catch (SQLException ignored) {
            }
    }

    @Override
    public void registerUser(String uid, String token) throws SQLException {

        StringBuilder query = new StringBuilder();
        uid = quote(uid);
        token = quote(token);
        String userStatus = quote(UserStatus.REGISTERED.toString());

        query.
                append("INSERT INTO ").
                append(TABLE_USERS).
                append(" (").
                append(COL_UID).append(",").
                append(COL_TOKEN).append(",").
                append(COL_USER_STATUS).
                append(")").
                append(" VALUES (").
                append(uid).append(",").
                append(token).append(",").
                append(userStatus).
                append(")");

        executeQuery(query.toString());
    }

    @Override
    public void registerUser(String uid, String token, String deviceModel, String androidVersion) throws SQLException {

        StringBuilder query = new StringBuilder();
        deviceModel = quote(deviceModel);
        androidVersion = quote(androidVersion);

        registerUser(uid, token);
        uid = quote(uid);

        query.
                append("UPDATE ").
                append(TABLE_USERS).
                append(" SET ").
                append(COL_DEVICE_MODEL).
                append("=").
                append(deviceModel).
                append(", ").
                append(COL_ANDROID_VERSION).
                append("=").
                append(androidVersion).
                append(" WHERE ").
                append(COL_UID).
                append("=").
                append(uid);

        executeQuery(query.toString());
    }

    @Override
    public void unregisterUser(String uid, String token) throws SQLException {

        StringBuilder query = new StringBuilder();
        token = quote(token);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sNow = quote(sdf.format(new Date()));
        String userStatus = quote(UserStatus.UNREGISTERED.toString());

        query.
                append("UPDATE ").
                append(TABLE_USERS).
                append(" SET ").
                append(COL_USER_STATUS).
                append("=").
                append(userStatus).append(",").
                append(COL_UNREGISTERED_DATE).
                append("=").
                append(sNow).
                append(" WHERE ").
                append(COL_UID).
                append("=").
                append(quote(uid)).
                append(" AND ").
                append(COL_TOKEN).
                append("=").
                append(token);

        executeQuery(query.toString());

        incrementColumn(TABLE_USERS, COL_UNREGISTERED_COUNT, COL_UID, uid);
    }

    @Override
    public UserRecord getUserRecord(String uid) throws SQLException {

        String query = "SELECT *" + " FROM " + TABLE_USERS + " WHERE " + COL_UID + "=" + quote(uid);
        Statement st = null;
        ResultSet resultSet = null;
        UserRecord record = null;

        try {
            initConn(); // Must init before each query since after 8 hours the connection is timed out
            st = _dbConn.createStatement();
            resultSet = st.executeQuery(query);
            while (resultSet.next()) {
                record = new UserRecord(
                        resultSet.getString(1),                                 // uid
                        resultSet.getString(2),                                 // token
                        resultSet.getDate(3),                                   // registered_date
                        Enum.valueOf(UserStatus.class, resultSet.getString(4)), // user_status
                        resultSet.getDate(5),                                   // unregistered_date
                        resultSet.getInt(6),                                    // unregistered_count
                        resultSet.getString(7),                                 // device_model
                        resultSet.getString(8)                                  // android_version
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (st != null)
                try {
                    st.close();
                } catch (SQLException ignored) {
                }
            if (resultSet != null)
                try {
                    resultSet.close();
                } catch (SQLException ignored) {
                }
            closeConn();
        }
        return record;
    }

    @Override
    public AppMetaRecord getAppMetaRecord() throws SQLException {

        StringBuilder query = new StringBuilder();

        query.
                append("SELECT ").append("*").
                append(" FROM ").append(TABLE_APP_META);

        Statement st = null;
        ResultSet resultSet = null;
        AppMetaRecord appMetaRecord = null;

        try {
            initConn(); // Must init before each query since after 8 hours the connection is timed out
            st = _dbConn.createStatement();
            resultSet = st.executeQuery(query.toString());
            double[] res = new double[2];
            if (resultSet.first()) {
                res[0] = resultSet.getDouble(1);
            }

            appMetaRecord = new AppMetaRecord(res[0]);

        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (st != null)
                try {
                    st.close();
                } catch (SQLException ignored) {
                }
            if (resultSet != null)
                try {
                    resultSet.close();
                } catch (SQLException ignored) {
                }
            closeConn();
        }
        return appMetaRecord;
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
    public void reRegisterUser(String uid, String token) throws SQLException {

        StringBuilder query = new StringBuilder();
        String userStatus = quote(UserStatus.REGISTERED.toString());

        query.
                append("UPDATE ").
                append(TABLE_USERS).
                append(" SET ").
                append(COL_TOKEN).
                append("=").
                append(quote(token)).append(",").
                append(COL_USER_STATUS).
                append("=").
                append(userStatus).
                append(" WHERE ").
                append(COL_UID).
                append("=").
                append(quote(uid));

        executeQuery(query.toString());
    }

    @Override
    public void reRegisterUser(String uid, String token, String deviceModel, String androidVersion) throws SQLException {

        StringBuilder query = new StringBuilder();
        reRegisterUser(uid, token);

        deviceModel = quote(deviceModel);
        androidVersion = quote(androidVersion);

        query.
                append("UPDATE ").
                append(TABLE_USERS).
                append(" SET ").
                append(COL_DEVICE_MODEL).
                append("=").
                append(deviceModel).
                append(", ").
                append(COL_ANDROID_VERSION).
                append("=").
                append(androidVersion).
                append(" WHERE ").
                append(COL_UID).
                append("=").
                append(uid);

        executeQuery(query.toString());
    }

    @Override
    public void updateUserRecord(String uid, UserRecord userRecord) throws SQLException {


        StringBuilder query = new StringBuilder("UPDATE " + TABLE_USERS);
        uid = quote(uid);
        String androidVersion = quote(userRecord.get_androidVersion());
        query.
                append(" SET ").
                append(COL_ANDROID_VERSION).
                append("=").
                append(androidVersion).
                append(" WHERE ").
                append(COL_UID).
                append("=").
                append(uid);

        executeQuery(query.toString());
    }

    @Override
    public void updateUserSmsVerificationCode(String uid, int code) throws SQLException {

        String query = "UPDATE " + TABLE_SMS_VERIFICATION + " SET " + COL_CODE + "=" + code + " WHERE " + COL_UID + "=" + quote(uid);
        executeQuery(query);
    }

    @Override
    public void updateAppRecord(double lastSupportedVersion) throws SQLException {

        //TODO Solve bug currentVersion as index
        StringBuilder query = new StringBuilder();

        query.
                append("UPDATE ").append(TABLE_APP_META).
                append(" SET ").
                append(COL_LAST_SUPPORTED_VER).
                append("=").
                append(lastSupportedVersion).
                append(" WHERE ").
                append(COL_LAST_SUPPORTED_VER).
                append(" > 0");

        executeQuery(query.toString());
    }

    @Override
    public int insertMediaTransferRecord(Map data) throws SQLException {

        FileManager managedFile = (FileManager) data.get(DataKeys.MANAGED_FILE);

        insertMediaFileRecord(
                data.get(DataKeys.MD5).toString(),
                managedFile.getFileExtension(),
                (int) managedFile.getFileSize(),
                COL_TRANSFER_COUNT);

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
                append(quote(data.get(DataKeys.SPECIAL_MEDIA_TYPE).toString())).append(",").
                append(quote(data.get(DataKeys.MD5).toString())).append(",").
                append(quote(data.get(DataKeys.SOURCE_ID).toString())).append(",").
                append(quote(data.get(DataKeys.DESTINATION_ID).toString())).
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
                append(visualMd5 != null ? quote(visualMd5) : null).append(",").
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
    public void insertUserSmsVerificationCode(String uid, int code) throws SQLException {

        StringBuilder query = new StringBuilder();
        uid = quote(uid);

        query.
                append("INSERT INTO ").
                append(TABLE_SMS_VERIFICATION).
                append(" (").
                append(COL_UID).append(",").
                append(COL_CODE).
                append(")").
                append(" VALUES (").
                append(uid).append(",").
                append(code).
                append(")");

        executeQuery(query.toString());
    }

    @Override
    public int getUserSmsVerificationCode(String uid) throws SQLException {

        String query = "SELECT " + COL_CODE + " FROM " + TABLE_SMS_VERIFICATION + " WHERE " + COL_UID + "=" + quote(uid);
        Statement st = null;
        ResultSet resultSet = null;
        int code = 0;

        try {
            initConn(); // Must init before each query since after 8 hours the connection is timed out
            st = _dbConn.createStatement();
            resultSet = st.executeQuery(query);
            if (resultSet.first())
                code = resultSet.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (st != null)
                try {
                    st.close();
                } catch (SQLException ignored) {
                }
            if (resultSet != null)
                try {
                    resultSet.close();
                } catch (SQLException ignored) {
                }
            closeConn();
        }
        return code;
    }

    @Override
    public List<MediaTransferRecord> getAllUserMediaTransferRecords(String uid) throws SQLException {

        String query = "SELECT *" + " FROM " + TABLE_MEDIA_TRANSFERS + " WHERE " + COL_UID_SRC + "=" + quote(uid);
        Statement st = null;
        ResultSet resultSet = null;
        List<MediaTransferRecord> records = new LinkedList<MediaTransferRecord>();

        try {
            initConn(); // Must init before each query since after 8 hours the connection is timed out
            st = _dbConn.createStatement();
            resultSet = st.executeQuery(query);
            while (resultSet.next()) {

                records.add(new MediaTransferRecord(
                        resultSet.getInt(1),    // transfer_id
                        Enum.valueOf(SpecialMediaType.class, resultSet.getString(2)), // type
                        resultSet.getString(3), // md5
                        resultSet.getString(4), // uid_src
                        resultSet.getString(5), // uid_dest
                        resultSet.getDate(6),   // datetime
                        (resultSet.getInt(7)!=0), // transfer_success
                        resultSet.getDate(8)    // transfer_datetime
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (st != null)
                try {
                    st.close();
                } catch (SQLException ignored) {
                }
            if (resultSet != null)
                try {
                    resultSet.close();
                } catch (SQLException ignored) {
                }
            closeConn();
        }
        return records;
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
                } catch (SQLException ignored) {
                }
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
                } catch (SQLException ignored) {
                }
            }
            closeConn();
        }
    }

    private String quote(String str) {
        return "\"" + str + "\"";
    }
    //endregion
}
