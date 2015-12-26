package DalObjects;

import java.sql.SQLException;

/**
 * Created by Mor on 16/11/2015.
 */
public interface IDAL {

    // Table users
    public static final String TABLE_USERS = "users";
    // Table keys under TABLE_USERS
    public static final String COL_UID = "uid";
    public static final String COL_TOKEN = "token";

    //Table communication_history
    public static final String TABLE_COMM_HISTORY = "communication_history";
    // Table keys under TABLE_COMM_HISTORY
    public static final String COL_COMM_ID = "comm_id";
    public static final String COL_UID_SRC = "uid_src";
    public static final String COL_UID_DEST = "uid_dest";
    public static final String COL_CONTENT_EXTENSION = "content_ext";
    public static final String COL_CONTENT_SIZE = "content_size";
    public static final String COL_TRANSFER_SUCCESS = "transfer_success";
    public static final String COL_TRANSFER_DATETIME = "transfer_datetime";


    boolean registerUser(String uid, String token);
    boolean unregisterUser(String uid);
    boolean updateUserPushToken(String uid, String token);
    int insertCommunicationHistory(String src, String dest, String extension, int size) throws SQLException;
    String getUserPushToken(String uid);
    void updateCommunicationRecord(int commId, String column, Object value) throws SQLException;
    void updateCommunicationRecord(int commId, String[] columns, Object[] values) throws SQLException;

}
