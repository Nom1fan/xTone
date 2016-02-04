package DalObjects;

import java.sql.SQLException;

/**
 * Created by Mor on 16/11/2015.
 */
public interface IDAL {

    // Table users
    String TABLE_USERS = "users";
    // Table keys under TABLE_USERS
    String COL_UID = "uid";
    String COL_TOKEN = "token";

    //Table communication_history
    String TABLE_COMM_HISTORY = "communication_history";
    // Table keys under TABLE_COMM_HISTORY
    String COL_TYPE = "type";
    String COL_COMM_ID = "comm_id";
    String COL_UID_SRC = "uid_src";
    String COL_UID_DEST = "uid_dest";
    String COL_CONTENT_EXTENSION = "content_ext";
    String COL_CONTENT_SIZE = "content_size";
    String COL_TRANSFER_SUCCESS = "transfer_success";
    String COL_TRANSFER_DATETIME = "transfer_datetime";


    void initConn() throws SQLException;
    void closeConn();
    boolean registerUser(String uid, String token);
    boolean unregisterUser(String uid, String token);
    boolean updateUserPushToken(String uid, String token);
    int insertCommunicationHistory(String type, String src, String dest, String extension, int size) throws SQLException;
    String getUserPushToken(String uid);
    void updateCommunicationRecord(int commId, String column, Object value) throws SQLException;
    void updateCommunicationRecord(int commId, String[] columns, Object[] values) throws SQLException;

}
