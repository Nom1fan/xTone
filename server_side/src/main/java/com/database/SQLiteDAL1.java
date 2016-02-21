//package com.database;
//
//import com.almworks.sqlite4java.SQLiteConnection;
//import com.almworks.sqlite4java.SQLiteException;
//import com.almworks.sqlite4java.SQLiteJob;
//import com.almworks.sqlite4java.SQLiteQueue;
//import com.almworks.sqlite4java.SQLiteStatement;
//
//import java.io.File;
//import java.sql.SQLException;
//
//import DalObjects.IDAL;
//
///**
// * Deprecated. Replaced by MySqlDAL
// */
//@Deprecated
//public class SQLiteDAL1 implements IDAL {
//
//    // *** General Database ***
//    public static final String GENERAL_DB_PATH = "\\server_side\\Database\\DB.db";
//
//    // Table uid2token
//    public static final String TABLE_UID2TOKEN = "uid2token";
//    // Table keys under TABLE_UID2TOKEN
//    public static final String COL_UID = "Uid";
//    public static final String COL_TOKEN = "Token";
//
//
//    private SQLiteQueue myQueue;
//
//    public SQLiteDAL1(String inputFile) throws SQLiteException {
//        SQLiteConnection db = new SQLiteConnection(new File(inputFile));
//        db.open(true);
//
//        myQueue = new SQLiteQueue(new File(inputFile));
//        myQueue.start();
//    }
//
//    public boolean createTable(String tableName, String ... colNames ) throws SQLiteException {
//
//        String query = "CREATE table " + tableName + " (";
//        for(int i=0;i<colNames.length;++i) {
//            query+=colNames[i];
//            if(i+1<colNames.length)
//                query+=",";
//        }
//        query+=")";
//
//        return execQuery(query);
//
//    }
//
//    @Override
//    public boolean registerUser(final String uid, final String token) {
//
//        String query = "INSERT INTO " + TABLE_UID2TOKEN + " values (" + "\"" + uid + "\"" + "," + "\"" + token + "\"" + ")";
//        return execQuery(query);
//    }
//
//    @Override
//    public boolean unregisterUser(final String uid) {
//
//        String query = "DELETE FROM "+TABLE_UID2TOKEN+" WHERE "+COL_UID+"="+"\""+uid+"\"";
//        return execQuery(query);
//    }
//
//    @Override
//    public boolean updateUserPushToken(final String uid, final String token) {
//
//        String query = "UPDATE " + TABLE_UID2TOKEN + " SET " + COL_TOKEN + "=" + "\"" + token + "\"" + " WHERE "+ COL_UID + "=" + "\"" +  uid + "\"";
//        return execQuery(query);
//    }
//
//    @Override
//    public int insertCommunicationHistory(String type, String src, String dest, String extension, int size) throws SQLException {
//        return 0;
//    }
//
//
//    @Override
//    public String getUserPushToken(final String uid) {
//
//        return myQueue.execute(new SQLiteJob<String>() {
//            protected String job(SQLiteConnection connection) throws SQLiteException {
//                String query = "SELECT "+COL_TOKEN+" FROM "+TABLE_UID2TOKEN+" WHERE "+COL_UID+"="+"\""+uid+"\"";
//                SQLiteStatement st = null;
//                String token = "";
//                try {
//                    st = connection.prepare(query);
//                    if(st.step())
//                        token = st.columnString(0);
//                } catch (SQLiteException e) {
//                    e.printStackTrace();
//                }
//                finally {
//                    if (st != null)
//                        st.dispose();
//                }
//                return token;
//            }
//        }).complete();
//    }
//
//    @Override
//    public void updateCommunicationRecord(int commId, String column, Object value) {
//
//    }
//
//    @Override
//    public void updateCommunicationRecord(int commId, String[] columns, Object[] values) {
//
//    }
//
//    private boolean execQuery(final String query) {
//
//        return myQueue.execute(new SQLiteJob<Boolean>() {
//            protected Boolean job(SQLiteConnection connection) throws SQLiteException {
//                Boolean isOK = false;
//
//                try {
//                    connection.exec(query);
//                    isOK = true;
//                } catch (SQLiteException e) {
//                    e.printStackTrace();
//                }
//
//                return isOK;
//            }
//        }).complete();
//
//    }
//}